package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.ExceptionDetailsExtractor;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.common.Reporters;
import org.cru.redegg.util.RedEggCollections;
import org.cru.redegg.util.RedEggStrings;
import org.cru.redegg.util.RedEggVersion;

/**
 * @author Matt Drees
 */
class RollbarPayloadBuilder
{
    private static final Joiner COMMA_JOINER = Joiner.on(",");

    private final RollbarConfig config;
    private final ErrorReport report;

    RollbarPayloadBuilder(
        RollbarConfig config,
        ErrorReport report)
    {
        this.config = config;
        this.report = report;
    }

    private static final String NOTIFIER_LANGUAGE = "java";


    Payload build() {

        Body.Builder body = new Body.Builder();
        body.bodyContent(getBody());
        Level level = determineLevel(report.getNotificationLevel());
        Data.Builder data = new Data.Builder();
        data.environment(config.getEnvironmentName());
        data.body(body.build())
            .title(buildTitle())
            .level(level)
            .platform(config.getPlatform())
            .language(NOTIFIER_LANGUAGE)
            .timestamp(toJavaTimestamp(getErrorTimestamp()));
        final ErrorLink errorLink = report.getErrorLink();
        if (errorLink != null)
        {
            RollbarErrorLink rollbarErrorLink = (RollbarErrorLink) errorLink;
            data.uuid(rollbarErrorLink.getId().toString());
        }

        if (report.getContext().containsKey("framework"))
        {
            String framework =
                RedEggCollections.flatten(report.getContext()).get("framework").toString();
            data.framework(framework);
        }

        // request data
        Request request = getRequestData();
        if (request != null)
        {
            data.request(request)
                .context(getContext());
        }

        // custom data
        data.custom(getCustomData());

        // person data
        Person personData = getPersonData();
        if (personData != null) {
            data.person(personData);
        }

        data.server(getServerData())
            .notifier(getNotifierData())
            .codeVersion(config.getCodeVersion());

        return new Payload.Builder()
            .accessToken(config.getAccessToken())
            .data(new TraceChainInverter().transform(data.build()))
            .build();
    }

    /**
     * Builds a more useful title from the exception chain than what Rollbar builds by default.
     * By default, it seems to just use the outer-most exception's class and message, separated by a colon.
     * The message is truncated after 255 characters.
     * Generally, the inner-most exception is more useful, and is what this logic prioritizes.
     */
    private String buildTitle() {
        final List<Throwable> thrown = report.getThrown();
        if (thrown.isEmpty())
        {
            return null;
        }
        Throwable first = thrown.get(0);
        List<Throwable> chain = Throwables.getCausalChain(first);
        final String title = Lists.reverse(chain)
            .stream()
            .map(this::shrinkThrowableString)
            .collect(Collectors.joining("; caused: "));

        return Ascii.truncate(title, 255, "...");
    }

    /** Strips out the package prefix from the throwable's class name, if it's there. */
    private String shrinkThrowableString(Throwable throwable) {
        return throwable.toString().replaceFirst("([a-z]+\\.)+(?=[A-Z])", "");
    }

    private long toJavaTimestamp(Instant date)
    {
        return date.toEpochMilli();
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

    private Instant getErrorTimestamp()
    {
        // TODO: record time of actual error.
        // Note: rollbar uses unix timestamps, but it accepts fractional second values.

        if (report.getWebContext() != null && report.getWebContext().getFinish() != null)
        {
            return report.getWebContext().getFinish();
        }
        else
        {
            return Instant.now();
        }
    }


    private BodyContent getBody() {
        List<Throwable> thrown = report.getThrown();
        if (!thrown.isEmpty()) {
            return new BodyFactory().from(new RollbarThrowableWrapper(thrown.get(0)), null).getContents();

//            //TODO: make this use a FilenameResolver, so that we can get github file links
//            return TraceChain.fromThrowable(thrown.get(0));

            //TODO: can we use this instead? pass the corresponding error log message?
//            return TraceChain.fromThrowable(report.getRootException(), description);
        } else {
            return new Message.Builder()
                .body(report.getRootErrorMessage().or("(message not available)"))
                .build();
        }
    }

    private Request getRequestData()
    {
        WebContext webContext = report.getWebContext();
        if (webContext == null)
        {
            return null;
        }

        Request.Builder requestData = new Request.Builder();

        requestData.url(webContext.getUrl().toString())
            .method(webContext.getMethod())
            .headers(flattenToCommaSeparatedValues(webContext.getHeaders()));

        //TODO: support routing params?
//        requestData.params();

        // params
        requestData.get(getQueryParametersAsMapOfLists(webContext));

        requestData.post(Collections.unmodifiableMap(
            flattenToCommaSeparatedValues(webContext.getPostParameters())));

        requestData.queryString(webContext.getQueryString());

        if (!Strings.isNullOrEmpty(webContext.getRemoteIpAddress()))
        {
            try
            {
                InetAddress address = InetAddress.getByName(webContext.getRemoteIpAddress());
                requestData.userIp(address.toString());
            }
            catch (UnknownHostException e)
            {
                // ignore
            }
        }

        // TODO: protocol ?

        // TODO: requestId ?

        requestData.body(webContext.getEntityRepresentation());

        ZonedDateTime start = ZonedDateTime.ofInstant(webContext.getStart(), ZoneId.systemDefault());

        Map<String, String > timing = new HashMap<>();
        timing.put("start", start.toString());
        if (webContext.getFinish() != null)
        {
            ZonedDateTime finish = ZonedDateTime.ofInstant(webContext.getFinish(), ZoneId.systemDefault());

            Duration duration = Duration.between(start, finish);
            timing.put("finish", finish.toString());
            timing.put("duration", duration.toString());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timing", timing);

        String responseStatus = webContext.getResponseStatus() == null ?
            "unknown" :
            webContext.getResponseStatus().toString();
        metadata.put("response_status_code", responseStatus);
        requestData.metadata(metadata);

        return requestData.build();
    }

    private Map<String, List<String>> getQueryParametersAsMapOfLists(WebContext webContext)
    {
        ArrayListMultimap<String, String> listMultimap =
            ArrayListMultimap.create(webContext.getQueryParameters());

        // two-step cast since the compiler can't do it in one step it seems
        Map<String, ?> rawMap = listMultimap.asMap();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> mapOfLists = (Map<String, List<String>>) rawMap;
        return mapOfLists;
    }

    private Map<String, String> flattenToCommaSeparatedValues(Multimap<String, String> multimap)
    {
        return Maps.transformValues(multimap.asMap(), COMMA_JOINER::join);
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
        Person.Builder personData = new Person.Builder().id(id);

        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, String> entry : user.entrySet())
        {
            if (entry.getKey().equals("email"))
            {
                personData.email(entry.getValue());
            }
            else if (entry.getKey().equals("username"))
            {
                personData.username(entry.getValue());
            }
            else if (!entry.getKey().equals("id"))
            {
                metadata.put(entry.getKey(), entry.getValue());
            }
        }
        personData.metadata(metadata);
        return personData.build();
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
        return new Notifier.Builder()
            .name("red-egg")
            .version(RedEggVersion.get())
            .build();
    }

    private Server getServerData() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostAddress", report.getLocalHostAddress());
        metadata.put("system_properties", report.getSystemProperties());
        metadata.put("environment_variables", report.getEnvironmentVariables());
        return new Server.Builder()
            .host(report.getLocalHostName())
            .branch(config.getBranch())
            .metadata(metadata)
            .build();
    }

}
