---
layout: post
title: "Example"
---
# MicroProfile System Test Framework

# Goals
1. Easy to get started
1. Work with any JavaEE or MicroProfile runtime
1. Provide true-to-production tests that are easy to write and fast to run

# How to use in an existing project:

Add jitpack.io repository configuration to your pom.xml:
```xml
<repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add `system-test` and `junit-jupiter` as test-scoped dependencies:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.dev-tools-for-enterprise-java</groupId>
        <artifactId>system-test</artifactId>
        <version>v0.1-alpha</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.4.2</version>
        <scope>test</scope>
    </dependency>

    <!-- other dependencies... -->
</dependencies>
```
