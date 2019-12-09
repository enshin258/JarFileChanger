
public class Main {
    public static void main(String[] args) throws NoClassFileException {

        // --i plik.jar --list-classes
        // --i plik.jar --list-ctors nazwa_klasy
        // --i plik.jar --script nazwa_skryptu --o jar wyjsciowy
        //TODO pomija plik .gitignore
        try {
            switch (args.length)
            {
                //possibly --i file.jar --list-something
                //possibly --i file.jar --list-something-in-class class.class
                case 3:
                case 4: {
                    if(args[0].equals("--i"))
                    {
                        Viewer.loadJarFile(args);
                    }
                    else
                    {
                        throw new NoInputParameterException();
                    }
                    break;
                }
                //possibly --i file.jar --script script.txt --o output.jar
                case 6:
                {
                    if(args[0].equals("--i"))
                    {
                       Scripter.loadJarFile(args);
                    }
                    else
                    {
                        throw new NoInputParameterException();
                    }
                    break;
                }
                default:
                {
                    throw new WrongNumberOfParametersException();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Exception: " + e.getClass() +  " catched");
        }

    }
}
