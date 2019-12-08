import javassist.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Scripter {
    //TODO NIE MOZNA W JEDNYM SKRYPCIE MODYFIKOWAC WIECEJ JAK RAZ TEGO SAMEGO PLIKU

    static JarFile inputFile = null;
    static File outputFile = null;

    static JarInputStream jarInputStream = null;
    static JarOutputStream jarOutputStream = null;

    static String[] arguments = null;
    static ClassPool classPool = ClassPool.getDefault();

    static File scriptFile = null;
    static LinkedList<String> elementsToIgnoreInNewJarFile = new LinkedList<>();


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
                String tempDirectoryName = tempDirectoryPath.substring(tempDirectoryPath.lastIndexOf('/') + 1).trim();
                tempDirectoryName +="/";
                tempDirectoryPath = tempDirectoryPath.substring(0,tempDirectoryPath.lastIndexOf("/")+1);
                JarEntry jarEntry = null;
                while((jarEntry=jarInputStream.getNextJarEntry())!=null)
                {
                    if(jarEntry.isDirectory() && jarEntry.getName().equals(tempDirectoryPath))
                    {
                        JarEntry jarNewDirectory = new JarEntry(jarEntry.getName()+tempDirectoryName);
                        jarOutputStream.putNextEntry(jarNewDirectory);
                        break;
                    }
                }
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
                byte[] bytes = ctClass.toBytecode();
                JarEntry classEntry = new JarEntry(ctClass.getName()+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
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
                byte[] bytes = ctClass.toBytecode();
                JarEntry classEntry = new JarEntry(ctClass.getName()+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
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
                CtClass ctClass = ClassPool.getDefault().get(wordsInScriptFile[1]);
                StringBuilder newMethodString = new StringBuilder();
                for(int i=2;i<wordsInScriptFile.length;i++)
                {
                    newMethodString.append(wordsInScriptFile[i]).append(" ");
                }
                CtMethod ctMethod = CtNewMethod.make(newMethodString.toString(),ctClass);
                ctClass.addMethod(ctMethod);
                byte[] bytes = ctClass.toBytecode();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                JarEntry classEntry = new JarEntry(tempJarEntryClassName+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");

                break;
            }
            case "remove-method":
            {
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                CtMethod ctMethod = ctClass.getDeclaredMethod(wordsInScriptFile[2]);
                ctClass.removeMethod(ctMethod);
                ctClass.writeFile();
                byte[] bytes = ctClass.toBytecode();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                JarEntry classEntry = new JarEntry(tempJarEntryClassName+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "set-method-body":
            {
                //TODO naprawic bo jak sie tworzy metode ktora w srodku powoluja jakas klase to wywala blad
//                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
//                ctClass.defrost();
//                CtMethod ctMethod = ctClass.getDeclaredMethod(wordsInScriptFile[2]);
//                StringBuilder newMethodBody= new StringBuilder();
//                try {
//                    File newMethodBodyFile = new File(wordsInScriptFile[3]);
//                    Scanner myReader = new Scanner(newMethodBodyFile);
//                    while (myReader.hasNextLine()) {
//                        newMethodBody.append(myReader.nextLine());
//                    }
//                    myReader.close();
//                } catch (FileNotFoundException e) {
//                    System.out.println("An error occurred.");
//                    e.printStackTrace();
//                }
//                ctMethod.setBody(newMethodBody.toString());
//                ctClass.writeFile();
//                byte[] bytes = ctClass.toBytecode();
//                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
//                JarEntry classEntry = new JarEntry(tempJarEntryClassName+".class");
//                jarOutputStream.putNextEntry(classEntry);
//                jarOutputStream.write(bytes);
//                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-before-method":
            {
                //TODO naprawic bo jak sie tworzy metode ktora w srodku powoluja jakas klase to wywala blad
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = ctClass.getDeclaredMethod(wordsInScriptFile[2]);
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
                byte[] bytes = ctClass.toBytecode();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                JarEntry classEntry = new JarEntry(tempJarEntryClassName+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-after-method":
            {
                //TODO naprawic bo jak sie tworzy metode ktora w srodku powoluja jakas klase to wywala blad
                CtClass ctClass = classPool.get(wordsInScriptFile[1]);
                ctClass.defrost();
                CtMethod ctMethod = ctClass.getDeclaredMethod(wordsInScriptFile[2]);
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
                byte[] bytes = ctClass.toBytecode();
                String tempJarEntryClassName = ctClass.getName().replaceAll("\\.","/");
                JarEntry classEntry = new JarEntry(tempJarEntryClassName+".class");
                jarOutputStream.putNextEntry(classEntry);
                jarOutputStream.write(bytes);
                elementsToIgnoreInNewJarFile.add(tempJarEntryClassName+".class");
                break;
            }
            case "add-field":
            {
                break;
            }
            case "remove-field":
            {
                break;
            }
            case "add-ctor":
            {
                break;
            }
            case "remove-ctor":
            {
                break;
            }
            case "set-ctor-body":
            {
                break;
            }
        }
    }

    public static void addRestOfJarExceptSomething() throws IOException {
        JarEntry entry = null;
        while ((entry = jarInputStream.getNextJarEntry())!=null) {
            if(!elementsToIgnoreInNewJarFile.contains(entry.getName()))
            {
                //TODO dodawanie pliku .gitignore
                if(entry.getRealName().equals(".gitignore"))
                {
                    System.out.println("znaleziono plik .gitignore");
                }
                if(entry.getName().equals(".gitignore"))
                {
                    System.out.println("znaleziono plik .gitignore");
                }
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
