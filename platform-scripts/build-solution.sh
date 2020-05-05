# Change Directory to solution on local machine
echo "iDAAS - Connect Clinical Industry Standards"
cd /Users/alscott/Development/IntelliJ/OpenSource-iDAAS/iDAAS-Connect-Clinical-IndustryStandards/

/usr/local/bin/mvn clean install
echo "Maven Build Completed"
/usr/local/bin/mvn package
echo "Maven Release Completed"
