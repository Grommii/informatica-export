# informatica-export

**informatica-export** is a command line util to export Informatica objects to separate .xml files. Also, in each file repository specific values replaced by default values. It is useful if you save this files under CVS.

Each file contains only one object with non-reusable dependencies that allow to store it only in one file.

### How to use it:
1. Set up Informatica query that returns list of objects you want to export.
2. Run **informatica-export** with required parameters.

### How it works:
Utility use Informatica command utils (pmrep) to execute requests against Informatica Repository.

Step 1: Connect to Informatica Repository (pmrep connect).

Step 2: Get the list of objects to export (pmrep executequery).

Step 3: Export each object in separate .xml file with folder structure:

     folder_name/
       object_type/
         object_subtype/
           object_name.xml

Step 4: Replace the repository specific values in .xml files with default placeholders (can be skipped by using -noreplace key).

The list of replaced values:

     CREATION_DATE="*" -> CREATION_DATE="01/01/2010 01:00:00"
     REPOSITORY NAME="*" -> REPOSITORY NAME="[INFA_REPOSITORY_NAME]"
     SERVERNAME ="*" -> SERVERNAME ="[INFA_INTEGRATION_SERVICE_NAME]"
     SERVER_DOMAINNAME ="*" -> SERVER_DOMAINNAME ="[INFA_DOMAIN_NAME]"
  
### Parameters
**informatica-export** runs with following parameters:

    -r <repository_name>
    -h <host_name> -o <port_number>
    -n <user_name>
    [-s <user_security_domain>]
    -x <password> | -X <password_environment_variable>
    -q <query_name>
    -cdir <commands_directory>
    -odir <output_directory>
    -dfile <domain_file_path>
    [-lfile <log_file_path>]
    [-noreplace]
    [-help]
    [-version]

- <repository_name> - Required. Informatica Repository name.
- <host_name> - Required. Gateway host name.
- <port_number>  - Required. Gateway port number.
- <user_name> - Required. User name used to connect to the repository.
- <user_security_domain>  - Optional. Name of the security domain that the user belongs to. Default is "Native".
- <password> - Required if using connection by plain password. User password.
- <password_environment_variable> - Required if using connection by password variable. User password environment variable.
- <query_name> - Required. Informatica query name.
- <commands_directory> - Required. Directory of Informatica commands. Note: don't use CommandLineUtilities folder, use \client\bin instead.
- <output_directory> - Required. Output directory.
- <domain_file_path> - Required. Full path to domains.infa file.
- <log_file_path> - Optional. Log directory. Default is the same as output directory.

Keys with no parameters:

- -noreplace - Optional. Skip the replacement of repository specific values in files.
- -help - Print help and exit.
- -version - Print version and exit.