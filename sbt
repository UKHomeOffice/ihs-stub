#!/bin/bash

name=sbt-launch
version=13.8
localJarName="${name}.jar"
remoteJarName="${name}-${version}.jar"
nexusUrl="https://artifact.dcj.ipt.homeoffice.gov.uk/nexus/service/local/repositories/thirdparty/content"
org="org/scala/sbt"
md5sum="00672c01d5beea62928e33cdeae7b46b"

filePresent() {
	[[ -f ${1} ]]
}
md5Match() {
	[[ "$(openssl md5 ${1}|awk '{print $2}')" == "${2}" ]]
}
downloadUrlAsFilename() {
	curl -o ${2} ${1}
}

if filePresent ${localJarName};then
	if md5Match ${localJarName} ${md5sum};then
		echo "Required ${name} jar already present and correct."
	else
		echo "Incorrect or corrupt ${name} jar present - removing..."
		rm -f ${localJarName}
	fi
fi

if ! filePresent ${localJarName} ]];then
	downloadUrlAsFilename "${nexusUrl}/${org}/${name}/${version}/${remoteJarName}" "${localJarName}"
	while ! md5Match ${localJarName} ${md5sum};do
		read response
		while [[ "${response}" != "n"
				&& "${response}" != "y"
				&& "${response}" != "N"
				&& "${response}" != "Y" ]];do
			echo -n "File did not download as expected, retry? y/n : "
			read response
			if [[ "${response}" == "n" || "${response}" == "N" ]];then
				echo "Okay."
				exit 1
			fi
		done
	done
fi



filePresent ~/.sbtconfig && . ~/.sbtconfig

java -ea                          \
  $SBT_OPTS                       \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -XX:+AggressiveOpts             \
  -XX:+UseParNewGC                \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:MaxPermSize=128M           \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -Xss8M                          \
  -Xms256M                        \
  -Xmx512M                        \
  -server                         \
  -jar ${localJarName} "$@"
