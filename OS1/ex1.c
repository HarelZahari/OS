#include <sys/types.h>
#include <sys/stat.h>

#include <fcntl.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define MAX_BUFFER_SIZE 65536
#define DESTINATION_FILE_MODE S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH

extern int opterr, optind;

void exit_with_usage(const char *message) 
{
	fprintf (stderr, "%s\n", message);
	fprintf (stderr, "Usage:\n\tex1 [-f] BUFFER_SIZE SOURCE DEST\n");
	exit(EXIT_FAILURE);
}

void copy_file(const char *source_file, const char *dest_file, int buffer_size, int force_flag) {
    /*
     * Copy source_file content to dest_file, buffer_size bytes at a time.
     * If force_flag is true, then also overwrite dest_file. Otherwise print error, and exit.
     *
     * TODO:
     * 	1. Open source_file for reading
     * 	2. Open dest_file for writing (Hint: is force_flag true?)
     * 	3. Loop reading from source and writing to the destination buffer_size bytes each time
     * 	4. Close source_file and dest_file
     *
     *  ALWAYS check the return values of syscalls for errors!
     *  If an error was found, use perror(3) to print it with a message, and then exit(EXIT_FAILURE)
     */
    char *currentBuffer = malloc(buffer_size * sizeof (char));
    int bufferReadSize;
    int writeStatus;
    int fdDest;
    int counter=0;
    int fdSource = open(source_file, O_RDONLY);
    
    //Check if bufferReadSize is invalid input
    if((bufferReadSize < 0) || (bufferReadSize > MAX_BUFFER_SIZE))
    {
        //Release buffer memory before exit
        free(currentBuffer);
        perror("Unable to write buffer content to destination file\n");
        exit(EXIT_FAILURE);
    }
    
    //Check if open Syscall failed to open source file
    if (fdSource == -1)
    {
        //Release buffer memory before exit
        free(currentBuffer);
        perror("Unable to open source file for reading\n");
        exit(EXIT_FAILURE);
    }

    //Check if force_flag is true and overwrite dest_file, create file if does not exist
     if (force_flag)
    {
        fdDest = open(dest_file, O_WRONLY | O_CREAT | O_TRUNC, DESTINATION_FILE_MODE);
    }
    else
    {
        fdDest = open(dest_file, O_WRONLY | O_CREAT | O_EXCL, DESTINATION_FILE_MODE);
    }

    //Check if open Syscall failed to open destination file
    if (fdDest == -1)
    {
        //A Syscalls to close the file descriptor of source file and release buffer memory before exit
        close(fdSource);
        free(currentBuffer);
        perror("Unable to open destination file for writing\n");
        exit(EXIT_FAILURE);
    }

    //Read to buffer from source file, until file end
    while ((bufferReadSize = read(fdSource, currentBuffer, buffer_size)) > 0)
    {
        //Check if read Syscall failed to read from source file
        if (bufferReadSize == -1)
        {
            //A Syscalls to close the file descriptor of both source and destination files and release buffer memory before exit
            close(fdDest);
            close(fdSource);
            free(currentBuffer);
            perror("Unable to read source file\n");
            exit(EXIT_FAILURE);
        }

        //Write in to destination file and get the Syscall status
        writeStatus = write(fdDest, currentBuffer, bufferReadSize);
        //Check if write Syscall failed to write in to destination file (not on first try)
        if ((writeStatus == -1 && counter!=0)||(writeStatus < bufferReadSize))
        {
            //A Syscalls to close the file descriptor of both source and destination files and release buffer memory before exit
            close(fdDest);
            close(fdSource);
            free(currentBuffer);
            perror("Unable to write buffer content to destination file\n");
            exit(EXIT_FAILURE);
        }
        //Check if write Syscall failed to write in to destination file (on first try)
        if (writeStatus == -1 && counter==0) 
        {
            //A Syscalls to close the file descriptor of both source and destination files and release buffer memory before exit
            close(fdDest);
            close(fdSource);
            free(currentBuffer);
            perror("Unable to write to destination file\n");
            exit(EXIT_FAILURE);
        }
        counter++;
    }
    //A Syscalls to close the file descriptor of both source and destination files
    int closeStatusSource = close(fdSource);
    int closeStatusDest = close(fdDest);
    //Check if close Syscall failed to close the source file
    if (closeStatusSource == -1) 
    {
        //A Syscalls to close the file descriptor of destination file and release buffer memory before exit
        close(fdDest);
        free(currentBuffer);
        perror("Unable to close source file\n");
        exit(EXIT_FAILURE);
    }
    //Check if close Syscall failed to close the destination file
    if (closeStatusDest == -1)
    {
        //Release buffer memory before exit
        free(currentBuffer);
        perror("Unable to close destination file\n");
        exit(EXIT_FAILURE);
    }
    //Successfully copied the all source file to destination and release buffer memory
    free(currentBuffer);
    printf("File %s was successfully copied to %s\n", source_file, dest_file);
    exit(EXIT_SUCCESS);

}

void parse_arguments(
        int argc, char **argv,
        char **source_file, char **dest_file, int *buffer_size, int *force_flag) {
    /*
     * parses command line arguments and set the arguments required for copy_file
     */
    int option_character;

    opterr = 0; /* Prevent getopt() from printing an error message to stderr */

    while ((option_character = getopt(argc, argv, "f")) != -1) {
        switch (option_character) {
            case 'f':
                *force_flag = 1;
                break;
            default: /* '?' */
                exit_with_usage("Unknown option specified");
        }
    }

    if (argc - optind != 3) {
        exit_with_usage("Invalid number of arguments");
    } else {
        *source_file = argv[argc - 2];
        *dest_file = argv[argc - 1];
        *buffer_size = atoi(argv[argc - 3]);

        if (strlen(*source_file) == 0 || strlen(*dest_file) == 0) {
            exit_with_usage("Invalid source / destination file name");
        } else if (*buffer_size < 1 || *buffer_size > MAX_BUFFER_SIZE) {
            exit_with_usage("Invalid buffer size");
        }
    }
}

int main(int argc, char **argv) {
	int force_flag = 0; /* force flag default: false */
	char *source_file = NULL;
        char *dest_file = NULL;
        int buffer_size = MAX_BUFFER_SIZE;

	parse_arguments(argc, argv, &source_file, &dest_file, &buffer_size, &force_flag);

	copy_file(source_file, dest_file, buffer_size, force_flag);

	return EXIT_SUCCESS;
}
