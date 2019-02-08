package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.Data;
import com.rollbar.payload.data.Level;
import com.rollbar.payload.data.Notifier;
import com.rollbar.payload.data.Person;
import com.rollbar.payload.data.Request;
import com.rollbar.payload.data.Server;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.data.body.BodyContents;
import com.rollbar.payload.data.body.Message;
import com.rollbar.payload.data.body.TraceChain;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.ExceptionDetailsExtractor;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.common.Reporters;
import org.cru.redegg.util.RedEggCollections;
import org.cru.redegg.util.RedEggStrings;
import org.cru.redegg.util.RedEggVersion;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Drees
 */
class RollbarPayloadBuilder
{
    private static final Joiner COMMA_JOINER = Joiner.on(",");

    private RollbarConfig config;
    private ErrorReport report;

    RollbarPayloadBuilder(
        RollbarConfig config,
        ErrorReport report)
    {
        this.config = config;
        this.report = report;
    }

    private static final String NOTIFIER_LANGUAGE = "java";


    Payload build() {

        Body body = new Body(getBody());
        Level level = determineLevel(report.getNotificationLevel());
        Data data = new Data(config.getEnvironmentName(), body)
            .level(level)
            .platform(config.getPlatform())
            .language(NOTIFIER_LANGUAGE)
            .timestamp(getErrorTimestamp());
        final ErrorLink errorLink = report.getErrorLink();
        if (errorLink != null)
        {
            RollbarErrorLink rollbarErrorLink = (RollbarErrorLink) errorLink;
            data = data.uuid(rollbarErrorLink.getId());
        }

        if (report.getContext().containsKey("framework"))
        {
            String framework =
                RedEggCollections.flatten(report.getContext()).get("framework").toString();
            data = data.framework(framework);
        }

        // request data
        Request request = getRequestData();
        if (request != null)
        {
            data = data.request(request)
                .context(getContext());
        }

        // custom data
        data = data.custom(getCustomData());

        // person data
        Person personData = getPersonData();
        if (personData != null) {
            data = data.person(personData);
        }

        data = data.server(getServerData())
            .notifier(getNotifierData())
            .codeVersion(config.getCodeVersion());

        return new Payload(config.getAccessToken(), data);
    }

    private Level determineLevel(NotificationLevel notificationLevel)
    {
        switch (notificationLevel)
        {
            case NONE:
                throw new IllegalArgumentException("report should not have level NONE");
            case WARNING:
                return Level.WARNING;
            case ERROR:
                return Level.ERROR;
            default:
                throw new IllegalArgumentException("unexpected level: " + notificationLevel);
        }
    }

    private String getContext()
    {
        Method component = report.getWebContext().getComponent();
        if (component == null)
        {
            return  null;
        }
        else
        {
            return component.getDeclaringClass().getSimpleName() +
                   "." + // or '#" ?
                   Reporters.buildSimplifiedMethodName(component);
        }
    }

    private Map<String, Object> getCustomData()
    {
        Map<String, Object> customData = RedEggCollections.flatten(report.getContext());

        List<Throwable> thrown = report.getThrown();
        if (thrown.size() > 1)
        {
            List<String> otherTraceChains = Lists.newArrayList();
            for (int i = 1; i < thrown.size(); i++)
            {
                Throwable throwable = thrown.get(i);
                otherTraceChains.add(Throwables.getStackTraceAsString(throwable));
            }
            customData.put("other_exceptions", Joiner.on("\n\n").join(otherTraceChains));
        }

        List<String> allDetails = Lists.newArrayList();
        ExceptionDetailsExtractor extractor = new ExceptionDetailsExtractor();
        for (Throwable throwable : thrown)
        {
            for (Throwable link : Throwables.getCausalChain(throwable))
            {
                allDetails.addAll(extractor.extractDetails(link));
            }
        }
        customData.put("exception_details", Joiner.on("\n").join(allDetails));

        customData.put("log_messages", Joiner.on("\n\n").join(report.getLogRecords()));
        return customData;
    }

    private Date getErrorTimestamp()
    {
        // TODO: record time of actual error.
        // However, rollbar only uses second-level precision, so it's not very helpful to be accurate.

        if (report.getWebContext() != null && report.getWebContext().getFinish() != null)
        {
            return report.getWebContext().getFinish().toDate();
        }
        else
        {
            return new Date();
        }
    }


    private BodyContents getBody() {
        List<Throwable> thrown = report.getThrown();
        if (!thrown.isEmpty()) {

            //TODO: make this use a FilenameResolver, so that we can get github file links
            return TraceChain.fromThrowable(thrown.get(0));

            //TODO: can we use this instead? pass the corresponding error log message?
//            return TraceChain.fromThrowable(report.getRootException(), description);
        } else {
            return new Message(report.getRootErrorMessage().or("(message not available)"));
        }
    }

    private Request getRequestData()
    {
        WebContext webContext = report.getWebContext();
        if (webContext == null)
        {
            return null;
        }

        Request requestData = new Request();

        requestData = requestData.url(webContext.getUrl().toString())
            .method(webContext.getMethod())
            .headers(flattenToCommaSeparatedValues(webContext.getHeaders()));

        //TODO: support routing params?
//        requestData.params();

        // params
        requestData = requestData.setGet(flattenToCommaSeparatedValues(webContext.getQueryParameters()));

        requestData = requestData.post(Collections.<String, Object>unmodifiableMap(
            flattenToCommaSeparatedValues(webContext.getPostParameters())));

        requestData = requestData.queryString(webContext.getQueryString());

        if (!Strings.isNullOrEmpty(webContext.getRemoteIpAddress()))
        {
            try
            {
                InetAddress address = InetAddress.getByName(webContext.getRemoteIpAddress());
                requestData = requestData.userIp(address);
            }
            catch (UnknownHostException e)
            {
                // ignore
            }
        }

        // TODO: protocol ?

        // TODO: requestId ?

        requestData = requestData.body(webContext.getEntityRepresentation());

        DateTime start = webContext.getStart();
        DateTime finish = webContext.getFinish();

        Duration  duration = new Duration(start, finish);

        Map<String, String> timing = new HashMap<>();
        timing.put("start", start.toString());
        if (finish != null)
        {
            timing.put("finish", finish.toString());
        }
        timing.put("duration", duration.toString());

        requestData = requestData.put("timing", timing);

        String responseStatus = webContext.getResponseStatus() == null ?
            "unknown" :
            webContext.getResponseStatus().toString();
        requestData = requestData.put("response_status_code", responseStatus);

        return requestData;
    }

    private Map<String, String> flattenToCommaSeparatedValues(Multimap<String, String> multimap)
    {
        return Maps.transformValues(multimap.asMap(), new Function<Collection<String>, String>()
        {
            @Nullable
            public String apply(Collection<String> input)
            {
                return COMMA_JOINER.join(input);
            }
        });
    }

    private Person getPersonData()
    {
        Map<String, String> user = report.getUser();
        if (user.isEmpty())
            return null;

        String id = determineId(user);
        if (id == null)
        {
            return null;
        }
        Person personData = new Person(id);

        for (Map.Entry<String, String> entry : user.entrySet())
        {
            if (entry.getKey().equals("email"))
            {
                personData = personData.email(entry.getValue());
            }
            else if (entry.getKey().equals("username"))
            {
                personData = personData.username(entry.getValue());
            }
            else if (!entry.getKey().equals("id"))
            {
                personData = personData.put(entry.getKey(), entry.getValue());
            }
        }
        return personData;
    }

    private String determineId(Map<String, String> user)
    {
        String idProperty = config.getIdentifyingUserProperty();
        if (idProperty != null)
        {
            String id = user.get(idProperty);
            if (id != null)
                return truncateId(id);
            else
                return null;
        }
        else if (user.containsKey("id"))
        {
            return truncateId(user.get("id"));
        }
        else
        {
            // TODO: log warning?
            return null;
        }
    }

    private String truncateId(String id)
    {
        return RedEggStrings.truncate(id, 40, "");
    }


    private Notifier getNotifierData() {
        return new Notifier()
            .name("red-egg")
            .version(RedEggVersion.get());
    }

    private Server getServerData() {
        return new Server()
            .host(report.getLocalHostName())
            .branch(config.getBranch())
            .put("hostAddress", report.getLocalHostAddress())
            .put("system_properties", report.getSystemProperties())
            .put("environment_variables", report.getEnvironmentVariables());
    }

}
