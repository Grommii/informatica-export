
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String PMREP_COMMAND = "pmrep";
    private static final String PMREP_CONNECT_COMMAND = "connect";
    private static final String PMREP_EXECUTEQUERY_COMMAND = "executequery";
    private static final String PMREP_OBJECTEXPORT_COMMAND = "objectexport";
    private static final String INFORMATICA_EXPORT_LOG_DEFAULT_FILENAME = "informatica-export.log";

    private static String commandsDirectory;
    private static String outputDirectory;

    private static String repositoryName;
    private static String repositoryDomainName;
    private static String repositoryHostname;
    private static String repositoryPort;
    private static String repositoryUsername;
    private static String repositoryPassword;
    private static String repositoryPasswordEnv;
    private static String repositorySecurityDomain;
    private static String repositoryQueryName;
    private static String repositoryDomainFilePath;

    private static String objectsListFileName = "objectlist.txt";
    private static String logFile;
    private static BufferedWriter logfileWriter;

    private static boolean printHelp = false;
    private static boolean printVersion = false;
    private static boolean replaceValues = true;

    public static void main(String[] args) throws Exception {

        collectArgs(args);
        if (printHelp){
            printHelp();
            return;
        }
        if (printVersion){
            printVersion();
            return;
        }
        checkArgs();

        Files.createDirectories(Paths.get(outputDirectory));

        logfileWriter = new BufferedWriter(new FileWriter(logFile));

        writeToConsole("Connecting to Repository.");
        int returnCodeConnect = runCommand(getConnectCommand());
        if (returnCodeConnect != 0){
            writeToConsole("Connection failed. Exit code: " + returnCodeConnect + ". Please check log: " + logFile);
            logfileWriter.close();
            return;
        }else{
            writeToConsole("Connection successful.");
        }

        writeToConsole("Getting the list of objects to export.");
        int returnCodeExecutequery = runCommand(getExecutequeryCommand());
        if (returnCodeExecutequery != 0){
            writeToConsole("Executequery command failed. Exit code: " + returnCodeExecutequery + ". Please check log: " + logFile);
            logfileWriter.close();
            return;
        }else{
            writeToConsole("List of objects received.");
        }

        File file = new File(outputDirectory + File.separator + objectsListFileName);
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {

            int currentObjectNumber = 1;
            while (reader.ready()) {
                String[] array = reader.readLine().split(",");
                String objectFolder = array[1];
                String objectName = array[2];
                String objectType = array[3];
                String objectSubType = array[4];

                // Fixing default type/subtype values for correct export
                if (objectType.equals("transformation") && objectSubType.equals("mapplet")) {
                    objectType = "mapplet";
                    objectSubType = "none";
                }

                String objectDirectory = outputDirectory + File.separator + objectFolder + File.separator + objectType;
                if (!objectSubType.equals("none")) {
                    objectDirectory += File.separator + objectSubType;
                }

                String objectFile = objectDirectory + File.separator + objectName + ".xml";

                Files.createDirectories(Paths.get(objectDirectory));

                int returnCodeObjectExport = runCommand(getObjectExportCommand(objectFolder, objectName, objectType, objectSubType, objectFile));
                if (replaceValues) {
                    replaceValues(objectFile);
                }
                writeToConsole((returnCodeObjectExport == 0 ? "SUCCESS" : "FAILED") + ". " + "Exported object " + currentObjectNumber + ": " + objectFolder + ", " + objectType + ", " + objectSubType + ", " + objectName + ". ");
                currentObjectNumber++;

            }
        }
        Files.delete(file.toPath());
        logfileWriter.close();

    }

    private static List<String> getObjectExportCommand(String objectFolder, String objectName, String objectType, String objectSubType, String objectFile) {
        List<String> objectExportCommand = new ArrayList<>();
        objectExportCommand.add(PMREP_COMMAND);
        objectExportCommand.add(PMREP_OBJECTEXPORT_COMMAND);
        objectExportCommand.add("-n");
        objectExportCommand.add(objectName);
        objectExportCommand.add("-o");
        objectExportCommand.add(objectType);
        objectExportCommand.add("-t");
        objectExportCommand.add(objectSubType);
        objectExportCommand.add("-f");
        objectExportCommand.add(objectFolder);
        objectExportCommand.add("-b");
        objectExportCommand.add("-u");
        objectExportCommand.add(objectFile);
        return objectExportCommand;
    }

    private static List<String> getExecutequeryCommand() {
        List<String> executequeryCommand = new ArrayList<>();
        executequeryCommand.add(PMREP_COMMAND);
        executequeryCommand.add(PMREP_EXECUTEQUERY_COMMAND);
        executequeryCommand.add("-q");
        executequeryCommand.add(repositoryQueryName);
        executequeryCommand.add("-u");
        executequeryCommand.add(outputDirectory + File.separator + objectsListFileName);
        return executequeryCommand;
    }

    private static List<String> getConnectCommand() {
        List<String> connectCommand = new ArrayList<>();
        connectCommand.add(PMREP_COMMAND);
        connectCommand.add(PMREP_CONNECT_COMMAND);
        connectCommand.add("-r");
        connectCommand.add(repositoryName);
        if (repositoryDomainName != null){
            connectCommand.add("-d");
            connectCommand.add(repositoryDomainName);
        } else {
            connectCommand.add("-h");
            connectCommand.add(repositoryHostname);
            connectCommand.add("-o");
            connectCommand.add(repositoryPort);
        }
        connectCommand.add("-n");
        connectCommand.add(repositoryUsername);
        if (repositorySecurityDomain != null){
            connectCommand.add("-s");
            connectCommand.add(repositorySecurityDomain);
        }
        if (repositoryPassword != null){
            connectCommand.add("-x");
            connectCommand.add(repositoryPassword);
        } else {
            connectCommand.add("-X");
            connectCommand.add(repositoryPasswordEnv);
        }
        return connectCommand;
    }

    private static void replaceValues(String filename) throws IOException{
        File file = new File(filename);

        String newFile = new String(Files.readAllBytes(file.toPath()));
        newFile = newFile.replaceAll("CREATION_DATE=\"[^\"]*\"","CREATION_DATE=\"01/01/2010 01:00:00\"");
        newFile = newFile.replaceAll("REPOSITORY NAME=\"[^\"]*\"","REPOSITORY NAME=\"[INFA_REPOSITORY_NAME]\"");
        newFile = newFile.replaceAll("SERVERNAME =\"[^\"]*\"","SERVERNAME =\"[INFA_INTEGRATION_SERVICE_NAME]\"");
        newFile = newFile.replaceAll("SERVER_DOMAINNAME =\"[^\"]*\"","SERVER_DOMAINNAME =\"[INFA_DOMAIN_NAME]\"");

        Files.write(file.toPath(),newFile.getBytes());
    }

    private static int runCommand(List<String> command) throws IOException, InterruptedException{
        command.set(0,commandsDirectory + File.separator + command.get(0));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (command.get(1).equals(PMREP_CONNECT_COMMAND)){
            processBuilder.environment().put("INFA_DOMAINS_FILE",repositoryDomainFilePath);
        }
        logfileWriter.write("Executing command:");
        logfileWriter.newLine();
        for (int i = 0; i < command.size(); i++) {
            if (!(command.get(1).equals(PMREP_CONNECT_COMMAND) && i > 1)){
                logfileWriter.write(command.get(i) + " ");
            }
        }
        logfileWriter.newLine();
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int exitValue = process.waitFor();
        while (bufferedReader.ready()){
            String line = bufferedReader.readLine();
            logfileWriter.write(line);
            logfileWriter.newLine();
        }
        logfileWriter.write("---------------------------");
        logfileWriter.newLine();
        logfileWriter.newLine();
        bufferedReader.close();
        return exitValue;
    }

    private static void writeToConsole(String s){
        System.out.print(new java.util.Date() + ": ");
        System.out.println(s);
    }

    private static void collectArgs(String[] args) throws Exception {

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-help")) {
                printHelp = true;
            } else if (arg.equals("-version")) {
                printVersion = true;
            } else if (arg.equals("-r")){
                repositoryName = args[i + 1];
                i++;
            } else if (arg.equals("-d")){
                repositoryDomainName = args[i + 1];
                i++;
            } else if (arg.equals("-h")){
                repositoryHostname = args[i + 1];
                i++;
            } else if (arg.equals("-o")){
                repositoryPort = args[i + 1];
                i++;
            } else if (arg.equals("-n")){
                repositoryUsername = args[i + 1];
                i++;
            } else if (arg.equals("-s")){
                repositorySecurityDomain = args[i + 1];
                i++;
            } else if (arg.equals("-x")){
                repositoryPassword = args[i + 1];
                i++;
            } else if (arg.equals("-X")){
                repositoryPasswordEnv = args[i + 1];
                i++;
            } else if (arg.equals("-q")){
                repositoryQueryName = args[i + 1];
                i++;
            } else if (arg.equals("-cdir")){
                commandsDirectory = args[i + 1];
                i++;
            } else if (arg.equals("-odir")){
                outputDirectory = args[i + 1];
                i++;
            } else if (arg.equals("-dfile")){
                repositoryDomainFilePath = args[i + 1];
                i++;
            } else if (arg.equals("-lfile")) {
                logFile = args[i + 1];
                i++;
            } else if (arg.equals("-noreplace")) {
                replaceValues = false;
            } else if (arg.startsWith("-")){
                printHelp();
                throw new Exception("Unknown argument: " + arg);
            }
        }

        if (logFile == null){
            logFile = outputDirectory + File.separator + INFORMATICA_EXPORT_LOG_DEFAULT_FILENAME;
        }

    }

    private static void checkArgs() throws Exception{
        String errString = "";
        if (repositoryName == null){
            errString = "Required option value missing: [-r]";
        } else if ( !(repositoryDomainName != null || (repositoryHostname != null && repositoryPort != null)) ){
            errString = "Required option value missing: [-h -o | -d]";
        } else if (repositoryUsername == null){
            errString = "Required option value missing: [-n]";
        } else if (repositoryPassword == null && repositoryPasswordEnv == null){
            errString = "Required option value missing: [-x | -X]";
        } else if (repositoryQueryName == null){
            errString = "Required option value missing: [-q]";
        } else if (commandsDirectory == null){
            errString = "Required option value missing: [-cdir]";
        } else if (outputDirectory == null){
            errString = "Required option value missing: [-odir]";
        } else if (repositoryDomainFilePath == null){
            errString = "Required option value missing: [-dfile]";
        }

        if (!errString.equals("")){
            printHelp();
            throw new Exception(errString);
        }

        if (repositoryDomainName != null && repositoryHostname != null){
            writeToConsole("Both -d and -h options specified. -d option value will be used.");
        }
        if (repositoryPassword != null && repositoryPasswordEnv != null){
            writeToConsole("Both -x and -X options specified. -x option value will be used.");
        }
    }

    private static void printHelp() {
        System.out.println("This utility export Informatica Powercenter objects");
        System.out.println("into separate .xml files");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("java -jar informatica-export.jar");
        System.out.println("  -r <repository_name>");
        System.out.println("  -h <host_name> -o <port_number>"); // " | -d <domain_name>" - failing to connect
        System.out.println("  -n <user_name>");
        System.out.println("  [-s <user_security_domain>]");
        System.out.println("  -x <password> | -X <password_environment_variable>");
        System.out.println("  -q <query_name>");
        System.out.println("  -cdir <commands_directory>");
        System.out.println("  -odir <output_directory>");
        System.out.println("  -dfile <domain_file_path>");
        System.out.println("  [-lfile <log_file_path>]");
        System.out.println("  [-noreplace]");
        System.out.println("  [-help]");
        System.out.println("  [-version]");
        System.out.println();
    }

    private static void printVersion() {
        System.out.println("informatica-export version " + "1.0.0");
    }

}
