package com.sap.ide.j2ee.servicelayer.archive;

import com.sap.ide.j2ee.adapter.ear.IApplication;
import com.sap.ide.j2ee.adapter.ear.IEarProject;
import com.sap.ide.j2ee.adapter.ear.IModule;
import com.sap.ide.j2ee.adapter.ext.core.archive.IArchiveProject;
import com.sap.ide.j2ee.extensionpoints.archive.EARAware;
import com.sap.ide.j2ee.extensionpoints.archive.IEarDescriptor;
import com.sap.ide.j2ee.servicelayer.ServicelayerPlugin;
import com.sap.ide.j2ee.servicelayer.archive.ArchiveAwareProjectsMap;
import com.sap.ide.j2ee.servicelayer.archive.ArchiveUtil;
import com.sap.ide.j2ee.servicelayer.archive.BuildArchive;
import com.sap.ide.j2ee.servicelayer.archive.BuildEntityResolver;
import com.sap.ide.j2ee.servicelayer.archive.EARGenerationException;
import com.sap.ide.j2ee.servicelayer.archive.J2eeArchiveDescriptionManager;
import com.sap.ide.j2ee.servicelayer.i18n.ServiceLayerMessages;
import com.sap.ide.j2ee.servicelayer.util.J2eeProjectManager;
import com.sap.ide.j2ee.servicelayer.util.vcs.dtr.DTRUtils;
import com.sap.ide.j2ee.util.project.ProjectUtil;
import com.tssap.tools.archive.ArchiveBuilder;
import com.tssap.tools.archive.Component;
import com.tssap.tools.archive.IArchive;
import com.tssap.tools.archive.IComponent;
import com.tssap.tools.archive.IProjectFileSet;
import com.tssap.tools.archive.Pattern;
import com.tssap.util.projectinfo.ProjectInfo;
import com.tssap.util.trace.TracerI;
import com.tssap.util.trace.TracingManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jst.j2ee.application.Application;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.internal.common.J2EEVersionUtil;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.DisplayName;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BuildEar extends BuildArchive {

   private static final String TASK__COLLECT_FILES_LOCATED_UNDERNEATH_META_INF = ServiceLayerMessages.BuildEar_collect_all_meta_inf_monitor_subtask;
   private static final String BUILD = "build";
   private static final String TASK__COLLECT_ALL_EAR_AWARE_FILES = ServiceLayerMessages.BuildEar_collect_all_ear_monitor_subtask;
   private static final String NOT_ABLE_TO_DELETE_OLD_ARCHIVEFILE = "Not able to delete old archivefile";
   private static final String TASK__DELETE_OLD_ARCHIVE_FILE = ServiceLayerMessages.BuildEar_delete_old_monitor_subtask;
   private static final String TASK__DISABLE_PRECOMPILE = ServiceLayerMessages.BuildEar_disable_precompile_monitor_subtask;
   private static final String TASK__SET_COMPONENT_BUILD_INFORMATION = ServiceLayerMessages.BuildEar_set_component_info_monitor_subtask;
   private static final String BUILD_EARARCHIVE_FAILED = "Build Eararchive failed";
   private static final String TASK__ENABLE_WORKSPACEMONITOR_FOR_BUILD_EAR_PROJECT = ServiceLayerMessages.BuildEar_enable_monitor_subtask;
   private static final String TASK__BUILD_EAR_ARCHIVE_FILE = ServiceLayerMessages.BuildEar_build_ear_file_monitor_subtask;
   private static final String TASK__DISABLE_WORKSPACEMONITOR_FOR_EAR_BUILD_PROCESS = ServiceLayerMessages.BuildEar_disable_monitor_subtask;
   private static final String TASK__COLLECT_ADDITIONAL_LIBRARIES = ServiceLayerMessages.BuildEar_collect_add_libs_monitor_subtask;
   private static final String TASK__BUILD_EAR_ARCHIVE = ServiceLayerMessages.BuildEar_build_ear_monitor_subtask;
   private static final TracerI tracer = TracingManager.getTracer(BuildEar.class);
   private static IEarDescriptor earArDescr = null;
   private static final String SDADD_FILE__PATH = "META-INF/sda-dd.xml";


   public static final void build(IProject prj, boolean buildTmp, boolean addJavaFiles, String location, String name, IProgressMonitor monitor) throws EARGenerationException {
      try {
         IArchiveProject e = ProjectUtil.getArchiveProject(prj);
         if(e != null) {
            e.setArchiveName(name);
         }

         sAddJavaFiles = addJavaFiles;
         BuildEar.PerformEarBuild performBuild = new BuildEar.PerformEarBuild(prj, buildTmp, location, name);
         performBuild.run(monitor);
      } catch (InvocationTargetException var8) {
         if(var8.getCause() instanceof EARGenerationException) {
            throw (EARGenerationException)var8.getCause();
         } else {
            throw new EARGenerationException("Build Eararchive failed", var8);
         }
      } catch (InterruptedException var9) {
         if(var9.getCause() instanceof EARGenerationException) {
            throw (EARGenerationException)var9.getCause();
         } else {
            throw new EARGenerationException("Build Eararchive failed", var9);
         }
      } catch (CoreException var10) {
         throw new EARGenerationException("Build Eararchive failed", var10);
      }
   }

   public static IEarDescriptor getEarDescriptor() {
      return earArDescr;
   }

   public static void setEarDescriptor(IEarDescriptor earDescriptor) {
      earArDescr = earDescriptor;
   }

   private static void setSDAInformation(IArchive archiveDescriptor, IProject prj, IProgressMonitor progressMonitor) {
      progressMonitor.subTask(TASK__SET_COMPONENT_BUILD_INFORMATION);
      String name = getNonDcAppName(prj);
      if(name == null || name.length() == 0 || name.indexOf(10) >= 0) {
         name = prj.getProject().getName();
      }

      archiveDescriptor.getComponentBuildInfo().getComponent().setName(name);
      String vendor = getVendor(prj);
      if(vendor != null && vendor.length() > 0) {
         archiveDescriptor.getComponentBuildInfo().getComponent().setVendor(vendor);
      }

      List dependecies = getComponentsList(prj);
      Iterator i$ = dependecies.iterator();

      while(i$.hasNext()) {
         IComponent dependency = (IComponent)i$.next();
         archiveDescriptor.getComponentBuildInfo().addDependency(dependency);
      }

   }

   public static String getNonDcAppName(IProject prj) {
      String name = null;
      boolean isEar14 = false;

      try {
         IFacetedProject provider = ProjectFacetsManager.create(prj);
         if(provider != null) {
            IProjectFacet mObj = ProjectFacetsManager.getProjectFacet("jst.ear");
            isEar14 = provider.hasProjectFacet(mObj.getVersion(J2EEVersionUtil.convertVersionIntToString(14)));
         }
      } catch (CoreException var8) {
         isEar14 = false;
      }

      if(isEar14) {
         EARArtifactEdit provider1 = new EARArtifactEdit(prj, true);
         Application mObj1 = provider1.getApplication();
         if(mObj1 != null) {
            name = provider1.getApplication().getDisplayName();
         }
      } else {
         IModelProvider provider2 = ModelProviderManager.getModelProvider(prj);
         if(provider2 != null) {
            Object mObj2 = provider2.getModelObject();
            if(name == null && mObj2 != null && mObj2 instanceof org.eclipse.jst.javaee.application.Application) {
               org.eclipse.jst.javaee.application.Application appl = (org.eclipse.jst.javaee.application.Application)mObj2;
               List displayNames = appl.getDisplayNames();
               if(displayNames != null && displayNames.size() > 0) {
                  DisplayName displayName = (DisplayName)displayNames.get(0);
                  name = displayName.getValue();
               }
            }
         }
      }

      return name;
   }

   private static void doInitialSteps(IProject earPrj, IProgressMonitor progressMonitor) {
      progressMonitor.subTask(TASK__DISABLE_PRECOMPILE);
      if("true".equals(ProjectInfo.getProperty(earPrj.getProject(), "j2ee.archive.EarPreBuildDisabled"))) {
         BuildArchive.setEclipePreCompileActive(false);
      }

      progressMonitor.subTask(TASK__DELETE_OLD_ARCHIVE_FILE);

      try {
         deleteOldArchivefile(earPrj, "/", earPrj.getName(), progressMonitor);
      } catch (CoreException var3) {
         tracer.error("build", "Not able to delete old archivefile", var3);
      }

   }

   public static ArchiveAwareProjectsMap getArchiveAwarePrjMap(IProject earPrj) {
      ArchiveAwareProjectsMap map = new ArchiveAwareProjectsMap();
      EARAware[] d = ServicelayerPlugin.getExternalEARAware();
      HashSet allReferencedProjects = new HashSet();
      IVirtualComponent createComponent = ComponentCore.createComponent(earPrj);
      IVirtualReference[] references = createComponent.getReferences();

      int i;
      for(i = 0; i < references.length; ++i) {
         IProject j = references[i].getReferencedComponent().getProject();
         if(j != null && j.isAccessible()) {
            allReferencedProjects.add(j);
         }
      }

      allReferencedProjects.addAll(Arrays.asList(J2EEProjectUtilities.getReferencingEARProjects(earPrj)));
      if(d != null && allReferencedProjects.size() != 0) {
         for(i = 0; i < d.length; ++i) {
            Iterator var11 = allReferencedProjects.iterator();

            while(var11.hasNext()) {
               IProject prj = (IProject)var11.next();
               if(d[i].isApplicable(prj)) {
                  d[i].setEarProject(earPrj);

                  try {
                     map.put(d[i], prj);
                  } catch (Exception var10) {
                     tracer.debug("Not able to put refernced project " + prj.getName() + " to archiveaware map \n ");
                  }
               }
            }
         }

         return map;
      } else {
         return map;
      }
   }

   private static void packEarAwareDeployables(ArchiveAwareProjectsMap map, IEarDescriptor earArDescr, IProgressMonitor progressMonitor) {
      progressMonitor.subTask(TASK__COLLECT_ALL_EAR_AWARE_FILES);
      collectArchiveAwareDeployableProjects(map, earArDescr, progressMonitor);
   }

   private static void pack_MetaInf_Files(IProject earPrj, IEarDescriptor earArDescr, IProgressMonitor pMonitor) {
      pMonitor.subTask(TASK__COLLECT_FILES_LOCATED_UNDERNEATH_META_INF);
      IProjectFileSet fs = J2eeArchiveDescriptionManager.getProjectFileSet();
      IContainer fld = ComponentCore.createComponent(earPrj).getRootFolder().getFolder("META-INF").getUnderlyingFolder();

      try {
         IFile e = (IFile)earPrj.getProject().findMember("META-INF/sda-dd.xml");
         if(e != null) {
            FileInputStream fis = new FileInputStream(e.getLocation().toPortableString());
            String sdadd_xml = ioToString(fis);
            earArDescr.getComponentBuildInfo().setDeployFileContent(sdadd_xml);
         }
      } catch (Throwable var8) {
         tracer.warning("Coud not read custom sda-dd.xml default one is generated", var8);
      }

      if(fld != null && fld.exists()) {
         fs.addProjectFiles(fld, Pattern.getPattern(Pattern.getPattern(3)), "META-INF");
         earArDescr.addFileSet(fs);
      }

   }

   private static Document getEngineXMLDocument(IProject project) {
      IEarProject earProject = J2eeProjectManager.getEarProject(project.getName());
      if(earProject == null) {
         earProject = J2eeProjectManager.getEarProject50(project.getName());
      }

      if(earProject != null) {
         IVirtualComponent vc = ComponentCore.createComponent(project);
         IVirtualFile sapEarDD = vc.getRootFolder().getFile("META-INF/application-j2ee-engine.xml");
         boolean exists = sapEarDD.exists();
         if(exists) {
            IFile f = sapEarDD.getUnderlyingFile();
            String applicationXML = f.getLocation().toOSString();
            File xml = new File(applicationXML);
            if(xml.isFile() && xml.canRead()) {
               try {
                  Document xmlDoc = buildDocument(new InputSource(applicationXML));
                  return xmlDoc;
               } catch (SAXException var10) {
                  tracer.error("Error while parsing application-j2ee-engine.xml", var10);
               } catch (IOException var11) {
                  tracer.error("Error while parsing application-j2ee-engine.xml", var11);
               } catch (ParserConfigurationException var12) {
                  tracer.error("Error while parsing application-j2ee-engine.xml", var12);
               }
            }
         }
      }

      return null;
   }

   public static String getVendor(IProject project) {
      Document xmlDoc = getEngineXMLDocument(project);
      String ret = null;
      if(xmlDoc != null) {
         Element el = xmlDoc.getDocumentElement();
         NodeList nodes = el.getElementsByTagName("provider-name");
         Node node = null;
         if(nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            node = nodes.item(0).getFirstChild();
            if(node != null) {
               ret = node.getNodeValue();
            }
         }
      }

      if(ret == null) {
         ret = "sap.com";
      }

      return ret;
   }

   public static List getComponentsList(IProject project) {
      Document xmlDoc = getEngineXMLDocument(project);
      ArrayList resultList = new ArrayList();
      if(xmlDoc == null) {
         return resultList;
      } else {
         Element el = xmlDoc.getDocumentElement();
         NodeList nodes = el.getElementsByTagName("reference-target");

         for(int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            NamedNodeMap attributes = node.getAttributes();
            Node vendorNode = attributes.getNamedItem("provider-name");
            Node appNameNode = node.getFirstChild();
            String vendorName = null;
            String appName = null;
            if(vendorNode != null) {
               vendorName = vendorNode.getNodeValue();
            }

            if(appNameNode != null) {
               appName = appNameNode.getNodeValue();
            }

            resultList.add(new Component(appName, vendorName));
         }

         return resultList;
      }
   }

   private static String ioToString(InputStream in) throws IOException {
      int aBuffSize = 1123123;
      String StringFromWS = "";
      byte[] buff = new byte[aBuffSize];
      ByteArrayOutputStream xOutputStream = new ByteArrayOutputStream(aBuffSize);

      int k;
      while((k = in.read(buff)) != -1) {
         xOutputStream.write(buff, 0, k);
      }

      StringFromWS = StringFromWS + xOutputStream.toString();
      return StringFromWS;
   }

   private static Document buildDocument(InputSource inputSource) throws SAXException, IOException, ParserConfigurationException {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      BuildEntityResolver resolver = new BuildEntityResolver();
      docBuilder.setEntityResolver(resolver);
      return docBuilder.parse(inputSource);
   }


   private static class PerformEarBuild implements IRunnableWithProgress {

      private static final String ERROR_WHILE_PACKAGING_AND_BUILDING_ANTARCHIVE = "Error while packaging and building AntArchive";
      private static final String COULD_NOT_BUILD_EAR__ = "Could not build ear: ";
      private IProject mEarProject;
      private String mArchiveLocation;
      private String mArchiveName;
      private IProgressMonitor mPMonitor;


      public PerformEarBuild(IProject prj, boolean buildTmp, String location, String name) {
         this.mEarProject = prj;
         this.mArchiveLocation = location;
         this.mArchiveName = name;
      }

      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
         try {
            if(monitor == null) {
               this.mPMonitor = new ProgressMonitorPart(new Composite(ServicelayerPlugin.getShell().getParent(), 0), (Layout)null);
            } else {
               this.mPMonitor = monitor;
            }

            this.build();
         } catch (EARGenerationException var3) {
            throw new InvocationTargetException(var3);
         }
      }

      private void build() throws EARGenerationException {
         try {
            if(this.mPMonitor == null) {
               this.mPMonitor = new NullProgressMonitor();
            }

            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            this.mPMonitor.beginTask(BuildEar.TASK__BUILD_EAR_ARCHIVE, -1);
            BuildEar.doInitialSteps(this.mEarProject, this.mPMonitor);
            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            if(BuildEar.earArDescr == null) {
               BuildEar.earArDescr = J2eeArchiveDescriptionManager.newEarDescription();
               BuildEar.earArDescr.setDestPath(this.mEarProject.getProject(), this.mArchiveLocation + this.mArchiveName);
            }

            BuildEar.earArDescr.setCompressed(true);
            BuildEar.setSDAInformation(BuildEar.earArDescr, this.mEarProject, this.mPMonitor);
            BuildEar.packEarAwareDeployables(BuildEar.getArchiveAwarePrjMap(this.mEarProject), BuildEar.earArDescr, this.mPMonitor);
            IVirtualFile ddfile = ComponentCore.createComponent(this.mEarProject).getRootFolder().getFile("META-INF/application.xml");
            if(ddfile != null && ddfile.exists()) {
               BuildEar.earArDescr.setEarDescriptor(ddfile.getUnderlyingFile());
            }

            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            BuildEar.pack_MetaInf_Files(this.mEarProject, BuildEar.earArDescr, this.mPMonitor);
            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            IApplication application = null;
            IEarProject earProject = J2eeProjectManager.getEarProject(this.mEarProject.getName());
            if(earProject == null) {
               earProject = J2eeProjectManager.getEarProject(this.mEarProject.getName(), 50);
            }

            if(earProject != null) {
               application = earProject.getApplication();
            }

            if(application != null) {
               IModule[] modules = application.getModules(8);
               if(modules != null) {
                  for(int i = 0; i < modules.length; ++i) {
                     IModule module = modules[i];
                     String altDd = module.getAltDd();
                     if(altDd != null) {
                        Path path = new Path(altDd);
                        IVirtualFile altDDFile = ComponentCore.createComponent(this.mEarProject).getRootFolder().getFile(path);
                        if(altDDFile.exists()) {
                           IFile underlyingFile = altDDFile.getUnderlyingFile();
                           String location = altDd;
                           if(altDd.startsWith("/")) {
                              location = altDd.substring(1);
                           }

                           BuildEar.earArDescr.addFile(underlyingFile, location);
                        }
                     }
                  }
               }
            }

            this.buildEar(BuildEar.earArDescr);
         } finally {
            BuildEar.earArDescr = null;
            BuildArchive.setEclipePreCompileActive(true);
         }

      }

      private final IEarDescriptor buildEar(IEarDescriptor ear) throws EARGenerationException {
         try {
            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            this.mPMonitor.subTask(BuildEar.TASK__COLLECT_ADDITIONAL_LIBRARIES);
            String e = null;
            if(J2EEVersionUtil.convertVersionStringToInt(J2EEProjectUtilities.getJ2EEProjectVersion(this.mEarProject)) == 50) {
               e = ArchiveUtil.getLibFolder(this.mEarProject);
            }

            ArchiveUtil.addReferencedEarLibs(ear, this.mEarProject.getProject(), e);
            this.mPMonitor.subTask(BuildEar.TASK__DISABLE_WORKSPACEMONITOR_FOR_EAR_BUILD_PROCESS);
            DTRUtils.disableWSMonitor(this.mEarProject.getProject());
            this.mPMonitor.subTask(BuildEar.TASK__BUILD_EAR_ARCHIVE_FILE);
            if(this.mPMonitor.isCanceled()) {
               throw new OperationCanceledException();
            }

            ArchiveBuilder.buildArchive(ear, this.mPMonitor);
            ArchiveUtil.createRepresentationInView(this.mEarProject.getProject());
            this.mPMonitor.done();
         } catch (Exception var6) {
            BuildEar.tracer.info("build", "Could not build ear: ");
            ear = null;
            throw new EARGenerationException("Error while packaging and building AntArchive", var6);
         } finally {
            this.mPMonitor.subTask(BuildEar.TASK__ENABLE_WORKSPACEMONITOR_FOR_BUILD_EAR_PROJECT);
            DTRUtils.enableWSMonitor(this.mEarProject.getProject());
            this.mPMonitor.done();
         }

         return ear;
      }
   }
}
