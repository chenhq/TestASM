 package com.github.chenhq.agent.util;
 
 //import com.newrelic.agent.android.Agent;
 import org.objectweb.asm.ClassReader;
 import org.objectweb.asm.Type;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileFilter;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.net.URLDecoder;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.jar.JarEntry;
 import java.util.jar.JarFile;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class Annotations
 {
   public static Collection<ClassAnnotation> getClassAnnotations(Class annotationClass, String packageSearchPath, Set<URL> classpathURLs)
   {
     String annotationDescription = 'L' + annotationClass.getName().replace('.', '/') + ';';
     Map fileNames = getMatchingFiles(packageSearchPath, classpathURLs);
 
     Collection list = new ArrayList();
     for (Map.Entry entry : fileNames.entrySet()) {
       String fileName = (String)entry.getKey();
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       try {
         Streams.copy(Annotations.class.getResourceAsStream('/' + fileName), out, true);
         ClassReader cr = new ClassReader(out.toByteArray());
         Collection annotations = ClassAnnotationVisitor.getAnnotations(cr, annotationDescription);
         list.addAll(annotations);
       }
       catch (IOException e) {
         e.printStackTrace();
       }
 
     }
 
     return list;
   }
 
   public static Collection<MethodAnnotation> getMethodAnnotations(Class annotationClass, String packageSearchPath, Set<URL> classpathURLs) {
     String annotationDescription = Type.getType(annotationClass).getDescriptor();
     Map fileNames = getMatchingFiles(packageSearchPath, classpathURLs);
 
     Collection list = new ArrayList();
     for (Map.Entry entry : fileNames.entrySet()) {
       String fileName = (String)entry.getKey();
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       try {
         Streams.copy(Annotations.class.getResourceAsStream('/' + fileName), out, true);
         ClassReader cr = new ClassReader(out.toByteArray());
         Collection annotations = MethodAnnotationVisitor.getAnnotations(cr, annotationDescription);
         list.addAll(annotations);
       }
       catch (IOException e) {
         e.printStackTrace();
       }
 
     }
 
     return list;
   }
 
   private static Map<String, URL> getMatchingFiles(String packageSearchPath, Set<URL> classpathURLs)
   {
     if (!packageSearchPath.endsWith("/")) {
       packageSearchPath = packageSearchPath + "/";
     }
     Pattern pattern = Pattern.compile("(.*).class");
     Map fileNames = getMatchingFileNames(pattern, classpathURLs);
     for (String file : (String[])fileNames.keySet().toArray(new String[0])) {
       if (!file.startsWith(packageSearchPath)) {
         fileNames.remove(file);
       }
     }
     return fileNames;
   }
 
   static Map<String, URL> getMatchingFileNames(Pattern pattern, Collection<URL> urls) {
     Map names = new HashMap();
     for (URL url : urls) {
       url = fixUrl(url);
       File file;
       try
       {
         file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
       }
       catch (UnsupportedEncodingException e) {
         e.printStackTrace();
         System.exit(1);
 
         return names;
       }
 
       if (file.isDirectory()) {
         List files = PatternFileMatcher.getMatchingFiles(file, pattern);
         for (File f : files) {
           String path = f.getAbsolutePath();
           path = path.substring(file.getAbsolutePath().length() + 1);
           names.put(path, url);
         }
       } else if (file.isFile()) {
         JarFile jarFile = null;
         try
         {
           jarFile = new JarFile(file);
 
           for (entries = jarFile.entries(); entries.hasMoreElements(); ) {
             JarEntry jarEntry = (JarEntry)entries.nextElement();
             if (pattern.matcher(jarEntry.getName()).matches())
               names.put(jarEntry.getName(), url);
           }
         }
         catch (IOException e)
         {
           Enumeration entries;
           e.printStackTrace();
           System.exit(1);
         }
         finally {
           if (jarFile != null)
             try {
               jarFile.close();
             } catch (IOException e) {
             }
         }
       }
     }
     return names;
   }
 
   private static URL fixUrl(URL url)
   {
     String protocol = url.getProtocol();
 
     if ("jar".equals(protocol)) {
       try {
         String urlString = url.toString().substring(4);
         int index = urlString.indexOf("!/");
         if (index > 0) {
           urlString = urlString.substring(0, index);
         }
         url = new URL(urlString);
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
     return url;
   }
 
   static URL[] getClasspathURLs() {
     ClassLoader classLoader = Agent.class.getClassLoader();
     if ((classLoader instanceof URLClassLoader)) {
       return ((URLClassLoader)classLoader).getURLs();
     }
     return new URL[0];
   }
 
   static class PatternFileMatcher
   {
     private final FileFilter filter;
     private final List<File> files = new ArrayList();
 
     public static List<File> getMatchingFiles(File directory, Pattern pattern) {
       PatternFileMatcher matcher = new PatternFileMatcher(pattern);
       directory.listFiles(matcher.filter);
       return matcher.files;
     }
 
     private PatternFileMatcher(final Pattern pattern)
     {
       this.filter = new FileFilter()
       {
         public boolean accept(File f)
         {
           if (f.isDirectory()) {
             f.listFiles(this);
           }
           boolean match = pattern.matcher(f.getAbsolutePath()).matches();
           if (match) {
             Annotations.PatternFileMatcher.this.files.add(f);
           }
           return match;
         }
       };
     }
   }
 }





