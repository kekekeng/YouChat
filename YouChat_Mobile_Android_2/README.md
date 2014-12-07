Instructions for building youchat-mobile-android

# Building Requires 
1.) Android SDK 
2.) Android-16 Target
3.) JDK 1.6+
4.) Apache Ant

# Update the youchat_mobile_android project to your enviroment
$ android update project --path .

# Update your local.properties file to include the following properties:
config.domain_nodejs=qa-youchat.acesse.com
config.domain_sip=qa-youchat.acesse.com

# Build debug and install into running emulator or device
$ ant debug install

# Build for dev, qa
$ ant distro
