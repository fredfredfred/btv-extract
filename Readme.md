# Build
```
brew install tessaract
gradle build
```

# Run
Gradle options contain this property so it can find tessaract library on mac os.
```
org.gradle.jvmargs=-Djna.library.path=/usr/local/lib
``` 

https://stackoverflow.com/questions/21394537/tess4j-unsatisfied-link-error-on-mac-os-x#30724844
