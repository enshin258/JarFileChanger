import javassist.*;
import javassist.bytecode.ClassFile;
import javassist.compiler.MemberResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class Viewer {

    static JarInputStream jarInputStream = null;
    static String[] arguments = null;
    static String actualClassname="";
    static ClassPool classPool = ClassPool.getDefault();

    public static void loadJarFile(String[] args) throws UnknownOperationException, NoClassFileException, NotFoundException, CannotCompileException {
        arguments=args;
        try {
            jarInputStream = new JarInputStream(new FileInputStream(arguments[1]));
            System.out.println("***Successfully loaded jar file***");
            System.out.println("Name of jar file: " + arguments[1]);
            checkIfNextOperationsOnThatJarFile();
        }
        catch (IOException e)
        {
            System.out.println("Unable to locate .jar file!");
        }

    }
    public static void checkIfNextOperationsOnThatJarFile() throws UnknownOperationException, IOException, NoClassFileException, NotFoundException, CannotCompileException {
        switch (arguments[2])
        {
            case "--list-packages":
            {
                listPackages();
                break;
            }
            case "--list-classes":
            {
                listClasses();
                break;
            }
            case "--list-methods":
            {
                loadClass();
                listMethodsInsideClass();
                break;
            }
            case "--list-fields":
            {
                loadClass();
                listFieldsInsideClass();
                break;
            }
            case "--list-ctors":
            {
                loadClass();
                listConstructorsInsideClass();
                break;
            }
            case "":
            {
                break;
            }
            default:
            {
                throw new UnknownOperationException();
            }
        }
    }
    public static void loadClass() throws IOException {
        System.out.println("***Loaded class: ***");
        actualClassname=arguments[3];
        System.out.println("Class name: " + actualClassname);

    }

    public static void listPackages() throws IOException {
        System.out.println("***Packages inside .jar file: ***");
        try {
            JarEntry jarEntry = null;
            if (jarInputStream != null) {
                while ((jarEntry =jarInputStream.getNextJarEntry()) != null) {

                    if (jarEntry.isDirectory()) {
                        System.out.println(jarEntry.getName());
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            jarInputStream.close();
        }
    }
    public static void listClasses() throws IOException {
        System.out.println("***Classes inside .jar file: ***");
        try {
            JarEntry jarEntry = null;
            if (jarInputStream != null) {
                while ((jarEntry =jarInputStream.getNextJarEntry()) != null) {
                    if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                        String className = jarEntry.getName();
//                        int lastSlash =  className.lastIndexOf("/");
//                        String justClassName = className.substring(lastSlash+1);
//                        System.out.println(justClassName);
                        System.out.println(className);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            jarInputStream.close();
        }
    }
    public static void listMethodsInsideClass() throws NotFoundException{

        classPool.insertClassPath(arguments[1]);
        CtClass ctClass = classPool.get(actualClassname);
        CtMethod[] methods = ctClass.getMethods();
        System.out.println("***Methods***");
        for(CtMethod method: methods){
            System.out.println(method.getName());
        }
    }
    public static void listFieldsInsideClass() throws NotFoundException {
        classPool.insertClassPath(arguments[1]);
        CtClass ctClass = classPool.get(actualClassname);
        CtField[] fields = ctClass.getFields();
        System.out.println("***Fields***");
        for(CtField field: fields){
            System.out.println(field.getName());
        }
    }
    public static void listConstructorsInsideClass() throws NotFoundException {
        classPool.insertClassPath(arguments[1]);
        CtClass ctClass = classPool.get(actualClassname);
        CtConstructor[] constructors = ctClass.getConstructors();

        System.out.println("***Constructors***");
        for(CtConstructor constructor: constructors){
            System.out.println(constructor.getName());
        }
    }


}
