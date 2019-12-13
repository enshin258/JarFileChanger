import javassist.*;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class Scripter {

    static JarFile inputFile = null;
    static File outputFile = null;

    static JarInputStream jarInputStream = null;
    static JarOutputStream jarOutputStream = null;

    static String[] arguments = null;
    static ClassPool classPool = ClassPool.getDefault();

    static File scriptFile = null;
    static Set<String> elementsToIgnoreInNewJarFile = new HashSet<>();
    static Set<CtClass> modifiedClasses = new HashSet<>();



    public static void loadJarFile(String[] args) throws UnknownOperationException, NoOutputParameterException, NotFoundException, CannotCompileException {
        arguments=args;
        classPool.insertClassPath(arguments[1]);
        try {
            inputFile = new JarFile(arguments[1]);
            jarInputStream = new JarInputStream(new FileInputStream(arguments[1]));
            System.out.println("***Successfully loaded jar file***");
            System.out.println("Name of jar file: " + arguments[1]);
            checkIfScriptExist();
            checkIfOutputFileGiven();
            loadCommandsInScriptFile();

        }
        catch (IOException | NoScriptInputException e)
        {
            e.printStackTrace();
            System.out.println("Unable to locate .jar file!");
        }

    }
    public static void checkIfScriptExist() throws NoScriptInputException, FileNotFoundException {

        if(arguments[2].equals("--script"))
        {
            scriptFile = new File(arguments[3]);
            System.out.println("***Successfully open script file***");
            System.out.println("Name of script file: " + scriptFile.getName());
        }
        else
        {
            throw new NoScriptInputException();
        }
    }
    public static void checkIfOutputFileGiven() throws NoOutputParameterException, IOException {
        if(arguments[4].equals("--o") && arguments[5].endsWith(".jar"))
        {
            System.out.println("***Successfully get name of output jar file***");
            System.out.println("Name of output jar file: " + arguments[5]);
            outputFile = new File(arguments[5]);
            jarOutputStream = new JarOutputStream(new FileOutputStream(outputFile),jarInputStream.getManifest());
        }
        else
        {
            throw new NoOutputParameterException();
        }
    }
    public static void loadCommandsInScriptFile() throws IOException, CannotCompileException, NotFoundException {
        try {
            Scanner myReader = new Scanner(scriptFile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] wordsInScriptFileInScriptFile = data.split(" ");
                System.out.println("***Actually executed command***");
                System.out.println(data);
                executeCommand(wordsInScriptFileInScriptFile);
            }
            addModifiedClasses();
            addRestOfJarExceptSomething();
            myReader.close();
            jarOutputStream.close();
            jarInputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("Script file not found");
        }
    }
    public static void executeCommand(String[] wordsInScriptFile) throws IOException, CannotCompileException, NotFoundException {
        switch (wordsInScriptFile[0])
        {
            case "add-package":
            {
                String tempDirectoryPath = wordsInScriptFile[1].replaceAll("\\.","/");
                JarEntry jarNewDirectory = new JarEntry(tempDirectoryPath  + "/");
                jarOutputStream.putNextEntry(jarNewDirectory);
                break;
            }
            case "remove-package":
            {
                String tempDirectory = wordsInScriptFile[1].replaceAll("\\.","/");
                tempDirectory+="/";
                elementsToIgnoreInNewJarFile.add(tempDirectory);
                break;
            }
            case "add-class":
            {
                String tempClassName = wordsInScriptFile[1].replaceAll("\\.","/");
                CtClass ctClass = classPool.makeClass(tempClassName);
                ctClass.defrost();
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempClassName + ".class");
                break;
            }
            case "remove-class":
            {
                String tempClass = wordsInScriptFile[1].replaceAll("\\.","/");
                tempClass+=".class";
                elementsToIgnoreInNewJarFile.add(tempClass);
                break;
            }
            case "add-interface":
            {
                String tempInterfaceName = wordsInScriptFile[1].replaceAll("\\.","/");
                CtClass ctClass = classPool.makeInterface(tempInterfaceName);
                ctClass.defrost();
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempInterfaceName + ".class");
                break;
            }
            case "remove-interface":
            {
                String tempInterface = wordsInScriptFile[1].replaceAll("\\.","/");
                tempInterface+=".class";
                elementsToIgnoreInNewJarFile.add(tempInterface);
                break;
            }
            case "add-method":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                StringBuilder newMethodString = new StringBuilder();
                for(int i=2;i<wordsInScriptFile.length;i++)
                {
                    newMethodString.append(wordsInScriptFile[i]).append(" ");
                }
                CtMethod ctMethod = CtNewMethod.make(newMethodString.toString(),ctClass);
                ctClass.addMethod(ctMethod);
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "remove-method":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = null;
                for (CtMethod c:ctClass.getMethods()) {
                    if(c.getLongName().equals(wordsInScriptFile[2]))
                    {
                        ctMethod = c;
                        break;
                    }
                }
                ctClass.removeMethod(ctMethod);
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "set-method-body":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = null;
                for (CtMethod c:ctClass.getMethods()) {
                    if(c.getLongName().equals(wordsInScriptFile[2]))
                    {
                        ctMethod = c;
                        break;
                    }
                }
                StringBuilder newMethodBody= new StringBuilder();
                try {
                    File newMethodBodyFile = new File(wordsInScriptFile[3]);
                    Scanner myReader = new Scanner(newMethodBodyFile);
                    while (myReader.hasNextLine()) {
                        newMethodBody.append(myReader.nextLine());
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                ctMethod.setBody(newMethodBody.toString());
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-before-method":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = null;
                for (CtMethod c:ctClass.getMethods()) {
                    System.out.println(c.getLongName());
                    if(c.getLongName().equals(wordsInScriptFile[2]))
                    {
                        ctMethod = c;
                        break;
                    }
                }
                StringBuilder newMethodBody= new StringBuilder();
                try {
                    File newMethodBodyFile = new File(wordsInScriptFile[3]);
                    Scanner myReader = new Scanner(newMethodBodyFile);
                    while (myReader.hasNextLine()) {
                        newMethodBody.append(myReader.nextLine());
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                ctMethod.insertBefore(newMethodBody.toString());
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-after-method":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = null;
                for (CtMethod c:ctClass.getMethods()) {
                    if(c.getLongName().equals(wordsInScriptFile[2]))
                    {
                        ctMethod = c;
                        break;
                    }
                }
                StringBuilder newMethodBody= new StringBuilder();
                try {
                    File newMethodBodyFile = new File(wordsInScriptFile[3]);
                    Scanner myReader = new Scanner(newMethodBodyFile);
                    while (myReader.hasNextLine()) {
                        newMethodBody.append(myReader.nextLine());
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                ctMethod.insertAfter(newMethodBody.toString());
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-field":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                StringBuilder newFieldString = new StringBuilder();
                for(int i=2;i<wordsInScriptFile.length;i++)
                {
                    newFieldString.append(wordsInScriptFile[i]).append(" ");
                }
                CtField ctField = CtField.make(newFieldString.toString(),ctClass);
                ctClass.addField(ctField);
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "remove-field":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtField ctField = ctClass.getField(wordsInScriptFile[2]);
                ctClass.removeField(ctField);
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-ctor":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                StringBuilder newConstructorString = new StringBuilder();
                for(int i=2;i<wordsInScriptFile.length;i++)
                {
                    newConstructorString.append(wordsInScriptFile[i]).append(" ");
                }
                CtConstructor ctConstructor = CtNewConstructor.make(newConstructorString.toString(),ctClass);
                ctClass.addConstructor(ctConstructor);
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "remove-ctor":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtConstructor ctConstructor = ctClass.getConstructor(wordsInScriptFile[2]);
                ctClass.removeConstructor(ctConstructor);
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "set-ctor-body":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtConstructor ctConstructor =null;
                for (CtConstructor c:ctClass.getConstructors()) {
                    System.out.println(c.getLongName());
                    if(c.getLongName().equals(wordsInScriptFile[2]))
                    {
                        ctConstructor = c;
                        break;
                    }
                }
                StringBuilder newConstructorBody= new StringBuilder();
                try {
                    File newMethodBodyFile = new File(wordsInScriptFile[3]);
                    Scanner myReader = new Scanner(newMethodBodyFile);
                    while (myReader.hasNextLine()) {
                        newConstructorBody.append(myReader.nextLine());
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                ctConstructor.setBody(newConstructorBody.toString());
                ctClass.writeFile();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                modifiedClasses.add(ctClass);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
        }
    }

    public static void addModifiedClasses() throws IOException, CannotCompileException {

        for (CtClass ctClass:modifiedClasses) {
            String tempClassname = ctClass.getName().replaceAll("\\.","/");
            JarEntry jarEntry = new JarEntry(tempClassname + ".class");
            byte[] bytes = ctClass.toBytecode();
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(bytes);
        }
    }


    public static void addRestOfJarExceptSomething() throws IOException {
        JarEntry entry = null;
        while ((entry = jarInputStream.getNextJarEntry())!=null) {
            if(!elementsToIgnoreInNewJarFile.contains(entry.getName())) {
                InputStream is = inputFile.getInputStream(entry);
                jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                while ((bytesRead = is.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, bytesRead);
                }
                is.close();
                jarOutputStream.flush();
                jarOutputStream.closeEntry();
            }
        }
    }
}
