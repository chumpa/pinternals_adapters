package com.sap.ide.j2ee.servicelayer.archive;

import com.sap.ide.j2ee.adapter.ra.IConnectorProject;
import com.sap.ide.j2ee.extensionpoints.archive.IEarDescriptor;
import com.sap.ide.j2ee.servicelayer.archive.ArchiveUtil;
import com.sap.ide.j2ee.servicelayer.archive.BuildArchive;
import com.sap.ide.j2ee.servicelayer.archive.J2eeArchiveDescriptionManager;
import com.sap.ide.j2ee.servicelayer.i18n.ServiceLayerMessages;
import com.sap.ide.j2ee.servicelayer.util.J2eeProjectManager;
import com.sap.ide.j2ee.servicelayer.util.vcs.dtr.DTRUtils;
import com.sap.ide.j2ee.util.project.ProjectUtil;
import com.tssap.tools.archive.ArchiveBuilder;
import com.tssap.tools.archive.ArchiveDescriptionManager;
import com.tssap.tools.archive.IAbsoluteFileSet;
import com.tssap.tools.archive.IArchive;
import com.tssap.tools.archive.IArchiveDescriptor;
import com.tssap.tools.archive.IMutablePattern;
import com.tssap.tools.archive.IPattern;
import com.tssap.tools.archive.IProjectFileSet;
import com.tssap.tools.archive.Pattern;
import com.tssap.util.trace.TracerI;
import com.tssap.util.trace.TracingManager;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

public class BuildRar extends BuildArchive {

   private static final String TASK_BUILD_RAR_ARCHIVE = ServiceLayerMessages.BuildRar_task_build_rar;
   private static final String TASK_BUILD_RA_PROJECT = ServiceLayerMessages.BuildRar_task_build_res_monitor_subtask;
   private static final String TASK_DELETE_OLD_ARCHIVE_FILE = ServiceLayerMessages.BuildRar_task_delete_old_monitor_subtask;
   private static final String TASK_COLLECT_ALL_FILES_LOCATED_UNDERNEATH_META_INF = ServiceLayerMessages.BuildRar_task_collect_all_meta_monitor_subtask;
   private static final String TASK_COLLECT_ALL_RELATED_RAR_FILES = ServiceLayerMessages.BuildRar_task_collect_all_rar_monitor_subtask;
   private static final String TASK_DISABLE_WORKSPACEMONITOR_FOR_RAR_BUILD_PROCESS = ServiceLayerMessages.BuildRar_task_disable_monitor_subtask;
   private static final String TASK_ENABLE_WORKSPACEMONITOR_FOR_RA_PROJECT = ServiceLayerMessages.BuildRar_task_enable_monitor_subtask;
   private static final TracerI tracer = TracingManager.getTracer(BuildRar.class);


   public static final void build(final IProject prj, final boolean buildTmp, final boolean addSourceFiles, final String location, final String name, IProgressMonitor monitor) throws InvocationTargetException {
      if(monitor == null) {
         monitor = new NullProgressMonitor();
      }

      if(((IProgressMonitor)monitor).isCanceled()) {
         throw new OperationCanceledException();
      } else {
         IRunnableWithProgress rwp = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
               try {
                  monitor.beginTask(BuildRar.TASK_BUILD_RAR_ARCHIVE, -1);
                  BuildArchive.sAddJavaFiles = addSourceFiles;
                  BuildRar.doWork(prj, buildTmp, location, name, monitor, true);
               } catch (Exception var3) {
                  throw new InvocationTargetException(var3);
               }
            }
         };

         try {
            rwp.run((IProgressMonitor)monitor);
         } catch (InterruptedException var8) {
            throw new InvocationTargetException(var8);
         }
      }
   }

   public static void createArchiveDescriptorForProjectWithPattern(Map allArchives, IJavaProject project, IPattern pattern) {
      try {
         IArchiveDescriptor e = J2eeArchiveDescriptionManager.newArchiveDescription();
         IProjectFileSet fs = J2eeArchiveDescriptionManager.getProjectFileSet();
         IPath outputPath = project.getOutputLocation();
         if(outputPath != null) {
            IResource res = project.getProject().findMember(outputPath);
            if(res == null) {
               res = project.getProject().getWorkspace().getRoot().findMember(outputPath);
            }

            if(res != null && res.exists() && (res instanceof IFolder || res instanceof IProject)) {
               fs.addProjectFiles((IContainer)res, pattern);
               e.addFileSet(fs);
               e.setDestPath(project.getProject(), "/" + ArchiveUtil.calculateDefaultJarName(project.getElementName()));
               if(true) {
                  IPattern javaPattern = Pattern.getPattern(3);
                  IClasspathEntry[] rawClasspath = project.getRawClasspath();

                  for(int i = 0; i < rawClasspath.length; ++i) {
                     IClasspathEntry entry = rawClasspath[i];
                     if(entry.getEntryKind() == 3) {
                        IFolder srcFolder = (IFolder)project.getProject().findMember(entry.getPath().removeFirstSegments(1));
                        fs.addProjectFiles(srcFolder, javaPattern);
                     }
                  }
               }

               allArchives.put(project, e);
            }
         }
      } catch (JavaModelException var12) {
         tracer.error("", var12);
      }

   }

   private static final void doWork(IProject prj, boolean buildTmp, String location, String name, IProgressMonitor pMonitor, boolean closeMonitor) throws Exception {
      if(pMonitor.isCanceled()) {
         throw new OperationCanceledException();
      } else {
         pMonitor.subTask(TASK_BUILD_RA_PROJECT);
         buildProjects(new IProject[]{prj}, pMonitor);
         if(pMonitor.isCanceled()) {
            throw new OperationCanceledException();
         } else {
            pMonitor.subTask(TASK_DELETE_OLD_ARCHIVE_FILE);

            try {
               deleteOldArchivefile(prj, "/", prj.getName() + ".rar", new NullProgressMonitor());
            } catch (CoreException var24) {
               tracer.error("Not Able to delete old archive", var24);
            }

            if(pMonitor.isCanceled()) {
               throw new OperationCanceledException();
            } else {
               IEarDescriptor earDescriptor = J2eeArchiveDescriptionManager.newEarDescription();
               HashMap map = new HashMap();
               IPattern allClassFilesPattern = Pattern.getPattern(1);
               IJavaProject javaProject = ProjectUtil.getJavaProject(prj);
               createArchiveDescriptorForProjectWithPattern(map, javaProject, allClassFilesPattern);
               IArchive[] archives = (IArchive[])((IArchive[])map.values().toArray(new IArchive[map.size()]));
               BuildArchive.addArchiveDescriptionToParticularArchive(earDescriptor, archives, (String[])null);
               earDescriptor.setDestPath(prj.getProject(), location + name);
               earDescriptor.setCompressed(true);
               if(pMonitor.isCanceled()) {
                  throw new OperationCanceledException();
               } else {
                  pMonitor.subTask(TASK_COLLECT_ALL_FILES_LOCATED_UNDERNEATH_META_INF);
                  pack_MetaInf_Files(prj, earDescriptor);
                  IProjectFileSet fs = ArchiveDescriptionManager.getProjectFileSet();
                  IVirtualFolder fld = ComponentCore.createComponent(prj).getRootFolder();
                  IMutablePattern pattern2 = Pattern.getPattern(Pattern.getPattern(3));
                  String contentFolder = fld.getUnderlyingFolder().getProjectRelativePath().segment(0);
                  String outputFolder = javaProject.getOutputLocation().segment(1);
                  String[] excludePatterns = new String[]{"**/*.rar*", ".settings/**", "**/*.project", "**/*.classpath", "**/plugin.xml", "**/*.java", "**/*.tssapinfo", contentFolder + "/**", "META-INF/**", outputFolder + "/**"};
                  pattern2.addExcludePattern(excludePatterns);
                  fs.addProjectFiles(prj.getProject(), pattern2);
                  earDescriptor.addFileSet(fs);
                  pMonitor.subTask(TASK_COLLECT_ALL_RELATED_RAR_FILES);
                  HashSet allRefProjects = new HashSet();
                  ProjectUtil.getReferencedProjectsForBuild(ProjectUtil.getJavaProject(prj), allRefProjects);
                  Iterator iter = allRefProjects.iterator();

                  while(iter.hasNext()) {
                     IJavaProject map1 = (IJavaProject)iter.next();
                     if(J2eeProjectManager.isJ2eeProject(map1.getProject())) {
                        iter.remove();
                     }
                  }

                  HashMap var25 = new HashMap(allRefProjects.size());
                  ArchiveUtil.createArchiveDescriptorsForProjects(var25, (IJavaProject[])((IJavaProject[])allRefProjects.toArray(new IJavaProject[allRefProjects.size()])));
                  addArchiveDescriptionToParticularArchive(earDescriptor, (IArchive[])((IArchive[])var25.values().toArray(new IArchive[0])), new String[0]);
                  if(pMonitor.isCanceled()) {
                     throw new OperationCanceledException();
                  } else {
                     IAbsoluteFileSet javaIOFileSet = J2eeArchiveDescriptionManager.getAbsoluteFileSet();
                     File[] allLibs = ProjectUtil.getReferencedLibsForBuild(javaProject);

                     for(int i = 0; i < allLibs.length; ++i) {
                        File currFile = allLibs[i];
                        if(currFile != null && currFile.exists()) {
                           javaIOFileSet.addAbsoluteFile(allLibs[i]);
                        }
                     }

                     earDescriptor.addFileSet(javaIOFileSet);
                     if(pMonitor.isCanceled()) {
                        throw new OperationCanceledException();
                     } else {
                        buildArchive(prj, buildTmp, earDescriptor, (IProjectFileSet)null, pMonitor, closeMonitor);
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean canCreateRar(IConnectorProject prj) {
      return prj.getConnector().getResourceAdapter() != null;
   }

   private static void pack_MetaInf_Files(IProject prj, IEarDescriptor ejbJarArDesc) {
      IProjectFileSet fs = ArchiveDescriptionManager.getProjectFileSet();
      IVirtualFolder fld = ComponentCore.createComponent(prj).getRootFolder().getFolder("META-INF");
      if(fld != null && fld.exists()) {
         fs.addProjectFiles(fld.getUnderlyingFolder(), Pattern.getPattern(Pattern.getPattern(3)), "META-INF");
         ejbJarArDesc.addFileSet(fs);
      }

   }

   private static IEarDescriptor buildArchive(IProject prj, boolean buildTmp, IEarDescriptor ejbJarArDesc, IProjectFileSet relatedFiles, IProgressMonitor pMonitor, boolean closeMonitor) throws Exception {
      try {
         if(!buildTmp) {
            pMonitor.subTask(TASK_DISABLE_WORKSPACEMONITOR_FOR_RAR_BUILD_PROCESS);
            DTRUtils.disableWSMonitor(prj);
            pMonitor.subTask(TASK_BUILD_RAR_ARCHIVE);
            ArchiveBuilder.buildArchive(ejbJarArDesc, pMonitor);
            ArchiveUtil.createRepresentationInView(prj);
         }
      } catch (Exception var10) {
         throw var10;
      } finally {
         pMonitor.subTask(TASK_ENABLE_WORKSPACEMONITOR_FOR_RA_PROJECT);
         DTRUtils.enableWSMonitor(prj);
         if(closeMonitor) {
            pMonitor.done();
         }

      }

      return ejbJarArDesc;
   }

}
