#define _GNU_SOURCE

//#include <glib.h>
// FIXME not too nice "solution"...
typedef int    gint;
typedef gint   gboolean;
#ifndef FALSE
#define FALSE   (0)
#endif
#ifndef TRUE
#define TRUE    (!FALSE)
#endif



#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <pwd.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <linux/un.h>
#include <unistd.h>
#include <errno.h>

#define PAGAVCS_DIRECTORY "/.pagavcs"
#define SOCKET_FILENAME "/.pagavcs/socket"
#define LOG_FILENAME "/.pagavcs/pagavcs.log"

void connectServer(int socket_fd, struct sockaddr_un address,
		char *fullLogFilename) {
	int first = 1;
	while (connect(socket_fd, (struct sockaddr *) &address,
			sizeof(struct sockaddr_un)) != 0) {
		if (first == 1) {

			char command[2048];

			strcpy(command, "nohup java -Xms16m");

			if (access("usr/share/pagavcs/data/pagavcs-bigmem", F_OK) != -1) {
				strcat(command, " -Xmx199m -XX:MaxPermSize=64M");
			} else {
				strcat(command, " -Xmx99m -XX:MaxPermSize=32M");
			}

			strcat(command, " -XX:PermSize=16M");
			strcat(command, " -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20");
			strcat(command, " -Djava.library.path=/usr/lib/jni -jar");
			strcat(command, " /usr/share/pagavcs/bin/pagavcs.jar >> ");
			strcat(command, fullLogFilename);
			strcat(command, " 2>&1 &");

			system(command);

			first = 0;
		}
		sleep(1);
	}
}

int main(int argc, char *argv[]) {
	int uid;
	struct passwd *pw;
	struct sockaddr_un address;
	int socket_fd, nbytes;
	int argIndex;
	char buffer[1024];
	char fullPagavcsDir[512];
	char fullSocketFilename[512];
	char fullLogFilename[512];
	char argument[512];
	char argumentToUse[512];
	char *newArgv[argc + 1];

if(	argc == 1 || strcmp(argv[1], "help") == 0
	|| strcmp(argv[1], "-help") == 0 || strcmp(argv[1], "-h") == 0
	|| strcmp(argv[1], "-?") == 0) {
		printf("usage: pagavcs [command] [argument(s)]\n");
		printf("  update           Update working copy.\n");
		printf("  commit           Show commit dialog for working copy.\n");
		printf("  log              Show log for working copy.\n");
		printf("  ignore           Add file or directory to ignore list.\n");
		printf(
		"  unignore         Remove file or directory from ignore list.\n");
		printf("  delete           Delete file or directory.\n");
		printf("  revert           Revert file or directory.\n");
		printf("  checkout         Checkout a working copy.\n");
		printf("  cleanup          Cleanup working copy.\n");
		printf("  resolve          Resolve conflict.\n");
		printf("  other            Display other window.\n");
		printf("  settings         Display settings window.\n");
		printf("  stop, exit, quit Stop PagaVCS server.\n");
		printf(
		"  getmenuitems     (For internal use.) Get menuitems for file or directory.\n");
		printf("  -w, -wait        Wait until command finished\n");
		printf(
		"  -c, -autoclose   Automatically close window after finished sucessfully\n");
		printf("  help, h\n");
		printf("  -h, --help       Display this help.\n");
		printf("\n");
		printf("Example:\n");
		printf(
		"  pagavcs update \"/home/johnny/big-project\" \"/home/johnny/mega-project\"\n");
		return 0;
	}

	uid = getuid();
	pw = getpwuid(uid);

	if (strcmp(pw->pw_name, "root") == 0) {
		printf("Running PagaVCS as root user is prohibited.\n");
		return -2;
	}

	strcpy(fullPagavcsDir, pw->pw_dir);
	strcat(fullPagavcsDir, PAGAVCS_DIRECTORY);

	mkdir(fullPagavcsDir, S_IRWXU);

	strcpy(fullSocketFilename, pw->pw_dir);
	strcat(fullSocketFilename, SOCKET_FILENAME);

	strcpy(fullLogFilename, pw->pw_dir);
	strcat(fullLogFilename, LOG_FILENAME);

	socket_fd = socket(PF_UNIX, SOCK_STREAM, 0);
	if (socket_fd < 0) {
		printf("socket() failed\n");
		return 1;
	}

	memset(&address, 0, sizeof(struct sockaddr_un));

	address.sun_family = AF_UNIX;
	strcpy(address.sun_path, fullSocketFilename);

	gboolean exitCommand = FALSE;
	// TODO check exit command

	connectServer(socket_fd, address, fullLogFilename);

	int foundNonOptions = 0;
	int argPhase = 0;
	argIndex = 1;
	while (argIndex < argc) {
		strcpy(argument, argv[argIndex]);

		if (argPhase == 0) {
			if (argument[0] != '-') {
				argPhase = 1;
			}
		}
		if (argPhase == 1) {
			foundNonOptions++;
		}
		argIndex++;
	}
	if (foundNonOptions < 2) {

		for (argIndex = 0; argIndex < argc; argIndex++) {
			newArgv[argIndex] = argv[argIndex];
		}
		newArgv[argc] = ".";
		argv = newArgv;
		argc++;
	}

	for (argIndex = 1; argIndex < argc; argIndex++) {
		write(socket_fd, "\"", strlen("\""));

		strcpy(argument, argv[argIndex]);
		if (argument[0] == '.') {
			strcpy(argumentToUse, canonicalize_file_name(argument));
		} else {
			strcpy(argumentToUse, argument);
		}

		write(socket_fd, argumentToUse, strlen(argumentToUse));
		write(socket_fd, "\"", strlen("\""));
		write(socket_fd, " ", strlen(" "));
	}
	write(socket_fd, "\n", strlen("\n"));

	nbytes = read(socket_fd, buffer, 256);
	buffer[nbytes] = 0;

	printf("%s\n", buffer);

	close(socket_fd);

	return 0;
}
