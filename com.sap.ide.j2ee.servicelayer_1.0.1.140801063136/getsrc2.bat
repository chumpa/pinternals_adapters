echo off
set JN=com.sap.ide.eclipse.deployer.ear2sda_1.0.0.140801063136.jar 
set JAR=C:\NWDSsp12.p3\plugins\%JN%
set FERN=C:\sap\fernflower.jar 
set UNZIP="C:\Program Files\7-Zip\7z.exe"

java -jar %FERN% %JAR%  .
rem mkdir src\com\sap\ide\j2ee\servicelayer\archive
rem %UNZIP% x %JN% com\sap\ide\j2ee\servicelayer\archive\BuildRar*java
rem move com\sap\ide\j2ee\servicelayer\archive\BuildRar*java src\com\sap\ide\j2ee\servicelayer\archive\
rem rmdir /s/q com
echo **************************************************************************
echo *                                                                        *
echo *                                                                        *
echo *                                                                        *
echo * Now fix BuildRar.createArchiveDescriptorForProjectWithPattern() method *
echo * look for "if(sAddJavaFiles)" condition                                 *
echo *                                                                        *
echo * if (sAddJavaFiles)                            =: if (true)             *
echo * IPattern javaPattern = Pattern.getPattern(2); =: .getPattern(3)        *
echo *                                                                        *
echo **************************************************************************
pause