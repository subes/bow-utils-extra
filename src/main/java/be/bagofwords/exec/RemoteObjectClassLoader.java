/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-5-3. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.exec;

import be.bagofwords.logging.Log;
import org.apache.commons.io.FileUtils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteObjectClassLoader extends ClassLoader {

    private final File compileDir;

    public RemoteObjectClassLoader(Object parent) {
        super(parent.getClass().getClassLoader());
        try {
            this.compileDir = Files.createTempDirectory("java_compile").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary root", e);
        }
    }

    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        if (!name.startsWith("java") && !name.startsWith("sun") && !name.startsWith("org.apache.log4j")) {
            String classPath = name.replaceAll("\\.", "/") + ".class";

            //First see if it a just compiled file
            File classFile = new File(compileDir, classPath);
            if (classFile.exists()) {
                try {
                    byte[] classBytes = FileUtils.readFileToByteArray(classFile);
                    // Log.i("Loaded class " + name);
                    return defineClass(name, classBytes, 0, classBytes.length);
                } catch (IOException exp) {
                    Log.w("Failed to read file " + classFile, exp);
                }
            }
        }

        // Log.i("NOT loading class " + name);
        return super.loadClass(name, resolve);
    }

    public synchronized void addRemoteClasses(Map<String, String> classSources) {
        try {
            List<File> sourceFiles = new ArrayList<>();
            boolean needsToCompileClasses = false;
            for (String className : classSources.keySet()) {
                String filePathNoExtension = getFilePathNoExtension(className);
                File sourceFile = new File(compileDir, filePathNoExtension + ".java");
                File parentDir = sourceFile.getParentFile();
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    throw new RuntimeException("Could not create directory " + parentDir.getAbsolutePath());
                }
                String classSource = classSources.get(className);
                if (sourceFile.exists()) {
                    String source = FileUtils.readFileToString(sourceFile, StandardCharsets.UTF_8);
                    if (!source.equals(classSource)) {
                        throw new RuntimeException("Received different sources for class " + className + "\n\n" + source + "\n\nAND\n\n" + classSource);
                    }
                } else {
                    FileUtils.write(sourceFile, classSource, StandardCharsets.UTF_8);
                    needsToCompileClasses = true;
                }
                sourceFiles.add(sourceFile);
            }
            if (!needsToCompileClasses) {
                return;
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            List<String> optionList = new ArrayList<>();
            optionList.add("-classpath");
            optionList.add(System.getProperty("java.class.path"));
            optionList.add("-proc:none");
            Writer writer = new StringWriter();

            Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            JavaCompiler.CompilationTask task = compiler.getTask(writer, fileManager, diagnostics, optionList, null, compilationUnit);
            boolean success = task.call();
            writer.close();
            if (!success) {
                StringBuilder errMessage = new StringBuilder();
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    errMessage.append(diagnostic);
                    errMessage.append("\n");
                }
                throw new RuntimeException("Failed to compile source files " + writer.toString() + " " + errMessage.toString().trim());
            }
        } catch (IOException exp) {
            throw new RuntimeException("Failed to compile source files", exp);
        }
    }

    private String getFilePathNoExtension(String className) {
        return className.replaceAll("\\.", File.separatorChar + "");
    }
}