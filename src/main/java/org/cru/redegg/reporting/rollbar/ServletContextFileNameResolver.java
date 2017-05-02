package org.cru.redegg.reporting.rollbar;

import org.cru.redegg.util.ErrorLog;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Matt Drees
 */
public class ServletContextFileNameResolver implements FileNameResolver
{

    ServletContext context;
    ErrorLog log;

    public ServletContextFileNameResolver(ServletContext context, ErrorLog log)
    {
        this.context = context;
        this.log = log;
    }

    @Override
    public String addPath(String fileName, String className)
    {
        if (!fileName.endsWith(".java"))
            return fileName;

        Class<?> aClass;
        ClassLoader classLoader = determineClassLoader();
        try
        {
            aClass = classLoader.loadClass(className);
        }
        catch (ClassNotFoundException e)
        {
            return fileName;
        }

        Package aPackage = aClass.getPackage();
        if (aPackage == null)
            return fileName;

        String packageName = aPackage.getName();

        String pathToClass = toPath(packageName) + getPublicClassForFile(fileName);
        String pathToClassFile = "/WEB-INF/classes/" + pathToClass;

        try
        {
            URL resource = context.getResource(pathToClassFile);
            if (resource == null)
            {
                //not part of the project
                return fileName;
            }
        }
        catch (MalformedURLException e)
        {
            log.error("servlet container does not like this path: " + pathToClassFile, e);
            return fileName;
        }

        return toPath(packageName) + fileName;
    }

    private ClassLoader determineClassLoader()
    {
        ClassLoader classLoader;
        if (context.getMajorVersion() >= 3)
        {
            classLoader = context.getClassLoader();
        }
        else
        {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }

    private String getPublicClassForFile(String fileName)
    {
        return fileName.replace(".java", ".class");
    }

    private String toPath(String packageName)
    {
        return packageName.replace('.', '/') + "/";
    }

}
