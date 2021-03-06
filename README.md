# Web of Needs parking bot

This bot is a pre step to build a smart city, where connected cars communicate with parking positions using 
the Web of Needs technology.

In order to work properly it is needed to implement a client software. Until there is a connected car standard, 
every provider can implement solutions based on there ecosystem and software stack.


The Bot is a  [Spring Boot Application](https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-running-your-application.html).

## Running the bot

### Prerequisites

- [Openjdk 8](https://adoptopenjdk.net/index.html) - the method described here does **not work** with the Oracle 8 JDK!
- Maven framework set up

### On the command line

```
cd bot-skeleton
export WON_NODE_URI="https://hackathonnode.matchat.org/won"
mvn clean package
java -jar target/bot.jar
```
Now go to [What's new](https://hackathon.matchat.org/owner/#!/overview) to find your bot, connect and [create an atom](https://hackathon.matchat.org/owner/#!/create) to see the bot in action.

### In Intellij Idea
1. Create a run configuration for the class `won.bot.skeleton.SkeletonBotApp`
2. Add the environment variables

  * `WON_NODE_URI` pointing to your node uri (e.g. `https://hackathonnode.matchat.org/won` without quotes)
  
  to your run configuration.
  
3. Run your configuration

If you get a message indicating your keysize is restricted on startup (`JCE unlimited strength encryption policy is not enabled, WoN applications will not work. Please consult the setup guide.`), refer to [Enabling Unlimited Strength Jurisdiction Policy](https://github.com/open-eid/cdoc4j/wiki/Enabling-Unlimited-Strength-Jurisdiction-Policy) to increase the allowed key size.

##### Optional Parameters for both Run Configurations:
- `WON_KEYSTORE_DIR` path to folder where `bot-keys.jks` and `owner-trusted-certs.jks` are stored (needs write access and folder must exist) 

## Start coding

Once the skeleton bot is running, you can use it as a base for implementing your own application. 

## Setting up
- Download or clone this repository
- Add config files

Please refer to the general [Bot Readme](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-bot/README.md) for more information on Web of Needs Bot applications.

## Collaboration
 * this bot uses [EasyCLI](https://github.com/WoN-Hackathon-2019/easycli) for chat command handling 
