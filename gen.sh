JOOQ_VERSION=3.5.0
M2_REPOS=~/.m2/repository;
JOOQ="$M2_REPOS/org/jooq";
MYSQL="$M2_REPOS/mysql/mysql-connector-java/5.1.33/mysql-connector-java-5.1.33.jar";
JARS="$JOOQ/jooq/$JOOQ_VERSION/jooq-$JOOQ_VERSION.jar:$JOOQ/jooq-meta/$JOOQ_VERSION/jooq-meta-$JOOQ_VERSION.jar:$JOOQ/jooq-codegen/$JOOQ_VERSION/jooq-codegen-$JOOQ_VERSION.jar:$MYSQL:.";

java -cp $JARS org.jooq.util.GenerationTool src/main/resources/gen.xml