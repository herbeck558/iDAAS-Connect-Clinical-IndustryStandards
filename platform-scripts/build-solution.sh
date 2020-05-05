# Change Directory to solution on local machine
_mydir="/Users/alscott/Development/IntelliJ/OpenSource-iDAAS/iDAAS-Connect-Clinical-IndustryStandards/"
cd _mydir
echo "The current directory: $_mydir"
# Run Maven Build
mvn clean install
echo "Maven Build Completed"
# Run Maven Release
mvn package
echo "Maven Release Completed"
