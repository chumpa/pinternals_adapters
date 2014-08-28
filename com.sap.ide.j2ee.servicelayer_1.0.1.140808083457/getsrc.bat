echo off
set JN=com.sap.ide.j2ee.servicelayer_1.0.1.140808083457.jar
set JAR=C:\NWDSsp12.p4\plugins\%JN%
set FERN=C:\sap\fernflower.jar 
set UNZIP="C:\Program Files\7-Zip\7z.exe"

java -jar %FERN% %JAR%  .
mkdir src\com\sap\ide\j2ee\servicelayer\archive
%UNZIP% x %JN% com\sap\ide\j2ee\servicelayer\archive\BuildRar*java
move com\sap\ide\j2ee\servicelayer\archive\BuildRar*java src\com\sap\ide\j2ee\servicelayer\archive\
rmdir /s/q com
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