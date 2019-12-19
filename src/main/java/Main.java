
public class Main {
    public static void main(String[] args) throws NoClassFileException {


        //listowanie np. metod klasy odbywa sie po wpisanu pelnej nazwy klasy bez dopisku .class
        //np. com.diamond.iain.javagame.entities.Aliens
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
