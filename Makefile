# Variables
JAVA = java
JAVAC = javac
CP = -cp lib/json-20230227.jar:src/
CP_RUN = -cp lib/json-20230227.jar:$(BIN_DIR)
SRC_DIR = src/main
BIN_DIR = bin
CPTEST = -cp lib/*:src/

# Define source files and their corresponding class files
SOURCES = $(SRC_DIR)/servers/AggregationServer.java \
          $(SRC_DIR)/servers/ContentServer.java \
          $(SRC_DIR)/servers/GETClient.java \
          $(SRC_DIR)/helpers/JSONParser.java \
          $(SRC_DIR)/helpers/LamportClock.java \
          $(SRC_DIR)/services/SocketService.java \
          $(SRC_DIR)/services/SocketServiceImpl.java

CLASSES = $(patsubst $(SRC_DIR)/%.java,$(BIN_DIR)/%.class,$(SOURCES))

TEST_SOURCES = src/tests/helpers/JSONParserTest.java \
							 src/tests/services/SocketServiceImplTest.java \
							 src/tests/servers/GETClientTest.java	

TEST_MAIN_CLASS = org.junit.platform.console.ConsoleLauncher

AGGREGATION = main.servers.AggregationServer
CONTENT = main.servers.ContentServer
GETCLIENT = main.servers.GETClient

$(shell mkdir -p $(BIN_DIR))

# Targets and their actions

all: $(CLASSES)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) $(CP) -d $(BIN_DIR) $<

# Compile JUnit test files
test_compile: $(TEST_SOURCES)
	$(JAVAC) $(CPTEST) -d $(BIN_DIR) $(TEST_SOURCES)

test_run: test_compile
	$(JAVA) $(CPTEST) $(TEST_MAIN_CLASS) --class-path $(BIN_DIR) --select-package tests

clean:
	rm -rf $(BIN_DIR)

aggregation: $(CLASSES)
	$(JAVA) $(CP_RUN) $(AGGREGATION)

content1: $(CLASSES)
	$(JAVA) $(CP_RUN) $(CONTENT) localhost:4567 weather1.txt

content2: $(CLASSES)
	$(JAVA) $(CP_RUN) $(CONTENT) localhost:4567 weather2.txt

client1: $(CLASSES)
	$(JAVA) $(CP_RUN) $(GETCLIENT) localhost:4567 IDS60901

client2: $(CLASSES)
	$(JAVA) $(CP_RUN) $(GETCLIENT) localhost:4567 IDS60902

client3: $(CLASSES)
	$(JAVA) $(CP_RUN) $(GETCLIENT) localhost:4567

.PHONY: clean all
