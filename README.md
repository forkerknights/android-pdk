# Unofficial Pinterest SDK Fork

This repository has been created for the purpose of providing
community-based support for those people who just can't wait
for Pinterest staff merge their Pull Requests. I will try my best to
actively maintain this repo with the help of the community.

For the updated README for this fork check out the [README_FORK.md](README_FORK.md)

**DISCLAIMER: This is not the official SDK. Use it at your own risk.**

## Original content below

# PinterestSDK for Android

The PinterestSDK for Android will allow you to authenticate an account with Pinterest and make requests on behalf of the authenticated user. For details on the supported endpoint, visit [the Pinterest API](https://developers.pinterest.com/docs/api/overview/).

## Installation

### Using AAR

1. Go to Android Studio | New Project | Minimum SDK

2. Select "API 11: Android 3.0" or higher and create your new project.

3. After you create a new project, go to File > New > New Module

4. Select "Import .JAR or >AAR Package"

5. Enter the path to .AAR file downloaded from this repo.

6. Under File > Project Structure, add pdk module as a dependency for your Project

7. Add the following as a dependency in build.gradle file

```java
compile 'com.mcxiaoke.volley:library:1.0.+'
```

### Using Source code

1. Go to Android Studio | New Project | Minimum SDK

2. Select "API 11: Android 3.0" or higher and create your new project.

3. After you create a new project, go to File > New > New Module

4. Select "Import Existing Project as Module"

5. Enter the path to source code downloaded from this repo.

6. Add the following as a dependency in build.gradle file

```java
compile 'com.mcxiaoke.volley:library:1.0.+'
```
7. If the pdk module is not added as a dependency automatically, add it manually under File > Project Structure.

## Setting up your App 

### Registering Your App
Visit the [Pinterest Developer Site](https://developers.pinterest.com/apps/) and register your application. This will generate an appId for you which you will need in the next steps.

### Configuring PDKClient
Before you make any calls using the PDKClient in your app, you will need to configure it with your appId: 

Add the following lines in "onCreate" method of your Launch Activity

```java
//imports
import com.pinterest.android.pdk.PDKCallback;
import com.pinterest.android.pdk.PDKClient;
import com.pinterest.android.pdk.PDKException;
import com.pinterest.android.pdk.PDKResponse;


//inside onCreate
PDKClient.configureInstance(this, YOUR_APP_ID);
PDKClient.getInstance().onConnect(this);
```

Add the following line outside the application tag in the manifest file.

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

And add the following Intent filter to your Login Activity

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="pdk[YOUR_APP_ID]" />
</intent-filter>
```

If your APP ID is 1234, scheme would be "pdk1234"


## Example App

The PDK comes with a Example App. Just clone this repo, add your AppId, and run to get started. 

## Documentation

For the full documentation and more information about the Pinterest Developer Platform, please visit: 

[PinterestSDK for Android](https://developers.pinterest.com/docs/sdks/android/)

[Pinterest API Docs](https://developers.pinterest.com/docs/getting-started/)

[Pinterest API signup](https://developers.pinterest.com/apps/)
