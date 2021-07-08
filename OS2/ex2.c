#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <curl/curl.h>
#include <string.h>
#include <signal.h>

#define HTTP_OK 200L
#define REQUEST_TIMEOUT_SECONDS 2L

#define URL_OK 0
#define URL_UNKNOWN -1
#define URL_ERROR -2

#define MAX_PROCESSES 1024

const char URL_PREFIX[] = "http";

typedef struct {
		double sum;
		int amount, unknown;
} ResultStruct ;


void usage() {
	fprintf(stderr, "usage:\n\t./ex2 num_of_processes FILENAME\n");
	exit(EXIT_FAILURE);
}

double check_url(const char *url) {
	CURL *curl;
	CURLcode res;
	double response_time = URL_UNKNOWN;

	curl = curl_easy_init();

	if(strncmp(url, URL_PREFIX, strlen(URL_PREFIX)) != 0){
		return URL_ERROR;
	}

	if(curl) {
		curl_easy_setopt(curl, CURLOPT_URL, url);
		curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
		curl_easy_setopt(curl, CURLOPT_TIMEOUT, REQUEST_TIMEOUT_SECONDS);
		curl_easy_setopt(curl, CURLOPT_NOBODY, 1L); /* do a HEAD request */

		res = curl_easy_perform(curl);
		if(res == CURLE_OK) {
			curl_easy_getinfo(curl, CURLINFO_NAMELOOKUP_TIME, &response_time);
		}

		curl_easy_cleanup(curl);

	}

	return response_time;

}

void serial_checker(const char *filename) {
	
	ResultStruct results = {0};

	FILE *toplist_file;
	char *line = NULL;
	size_t len = 0;
	ssize_t read;
	double res;

	toplist_file = fopen(filename, "r");

	if (toplist_file == NULL) {
		exit(EXIT_FAILURE);
	}

	while ((read = getline(&line, &len, toplist_file)) != -1) {
		if (read == -1) {
			perror("unable to read line from file");
		}
		line[read-1] = '\0'; /* null-terminate the URL */
		if (URL_UNKNOWN == (res = check_url(line))) {
			results.unknown++;
		}
		else if(res == URL_ERROR){
			printf("Illegal url detected, exiting now\n");
			exit(0);
		}
		else {
			results.sum += res;
			results.amount++;
		}
	}

	free(line);
	fclose(toplist_file);

	if(results.amount > 0){
		printf("%.4f Average response time from %d sites, %d Unknown\n",
						results.sum / results.amount,
						results.amount,
						results.unknown);
	}
	else{
		printf("No Average response time from 0 sites, %d Unknown\n", results.unknown);
	}
}
/**
 * @define - handle single worker that run on child process
 */
void worker_checker(int worker_id, int num_of_workers, const char *filename, int pipe_write_fd) {
    /*
     * TODO: this checker function should operate almost like serial_checker(), except:
     * 1. Only processing a distinct subset of the lines (hint: think Modulo)
     * 2. Writing the results back to the parent using the pipe_write_fd (i.e. and not to the screen)
     * 3. If an URL_ERROR returned, all processes (parent and children) should exit immediatly and an error message should be printed (as in 'serial_checker')
     */

    ResultStruct results = {0};

    double res;    
    char *line = NULL;
    size_t len = 0;
    ssize_t read;
    int line_number = 0;
    FILE *toplist_file = fopen(filename, "r");

    // Check if success to open file
    if (toplist_file == NULL) {
        perror("Unable to open file\n");
        exit(EXIT_FAILURE);
    }

    // Go over all the lines
    while ((read = getline(&line, &len, toplist_file)) != -1) {               
        // Check if success to read line
        if (read == -1) {            
            perror("Unable to read line from file\n");            
        }
        line[read-1] = '\0'; /* null-terminate the URL */
        // Check URL line only if it's belong to this child process
        if (line_number % num_of_workers == worker_id) {  
          // The url_check function finish with URL unkown result
          if (URL_UNKNOWN == (res = check_url(line))) {
			results.unknown++;
		}
                // URL is not on the required format
		else if(res == URL_ERROR){
                        pid_t fatherPID=getppid();
			printf("Illegal url detected, exiting now\n");
                        // Kill the  father process and all of his children
                        kill(-fatherPID,SIGKILL);
			exit(0);
		}
		else { // The url_check function finish with success
			results.sum += res;
			results.amount++;
		}
        }
        
        line_number++;
    }
    
    // Free memory
    free(line);
    fclose(toplist_file);    

    // Write the results back to the parent using the pipe_write_fd
    int writeStatus = write(pipe_write_fd, &results, sizeof(results));
    if (writeStatus == -1) {        
        perror("Unable to write pipeline into parent process\n");
        exit(EXIT_FAILURE);
    }    
}

/**
 * Handle separate the work between process and merge the results
 */
void parallel_checker(int num_of_processes, const char *filename) {
    int worker_id;
    // We use two pipes the first for reading, second for writing
    int pipefd[2];
    int closeFirstPipeStatus;
    int closeSecondPipeStatus;

    ResultStruct results = {0};
    ResultStruct results_buffer = {0};

    // initialize  pipe
    pipe(pipefd);

    // Start num_of_processes new workers
    for (worker_id = 0; worker_id < num_of_processes; ++worker_id) {
        if (fork() == 0) // Check if it's a child process
        {
            // Close the pipe for reading
            closeFirstPipeStatus = close(pipefd[0]);
            if (closeFirstPipeStatus == -1) {
                perror("Unable to close pipefd [0]\n");
                exit(EXIT_FAILURE);
            }

            // Current child process work on his part of the file
            worker_checker(worker_id, num_of_processes, filename, pipefd[1]);

            // Close the pipe for writing
            closeSecondPipeStatus = close(pipefd[1]);
            if (closeSecondPipeStatus == -1) {
                perror("Unable to close pipefd[1]\n");
                exit(EXIT_FAILURE);
            }
            exit(EXIT_SUCCESS);
        }
    }

    wait(NULL); // Wait for all children to terminate

    // Close the pipe for writing
    closeSecondPipeStatus = close(pipefd[1]);
    if (closeSecondPipeStatus == -1) {
        perror("Unable to close pipefd[1]\n");
        exit(EXIT_FAILURE);
    }

    // Collect data results from all children process
    for (worker_id = 0; worker_id < num_of_processes; ++worker_id) {
        // Read from pipe line
        if (read(pipefd[0], &results_buffer, sizeof (results_buffer)) == -1) {
            perror("Unable to read from pipefd[0]\n");
            exit(EXIT_FAILURE);
        } else {
            results.amount += results_buffer.amount;
            results.sum += results_buffer.sum;
            results.unknown += results_buffer.unknown;
        }

    }

    // Close the pipe for reading
    closeFirstPipeStatus = close(pipefd[0]);
    if (closeFirstPipeStatus == -1) {
        perror("Unable to close pipefd [0]\n");
        exit(EXIT_FAILURE);
    }

    // print the total results
    if (results.amount > 0) {
        printf("%.4f Average response time from %d sites, %d Unknown\n",
                results.sum / results.amount,
                results.amount,
                results.unknown);
    } else {
        printf("No Average response time from 0 sites, %d Unknown\n", results.unknown);
    }
    exit(EXIT_SUCCESS);

}

int main(int argc, char **argv) {
	if (argc != 3) {
		usage();
	} else if (atoi(argv[1]) == 1) {
		serial_checker(argv[2]);
	} else parallel_checker(atoi(argv[1]), argv[2]);
   

	return EXIT_SUCCESS;
}
