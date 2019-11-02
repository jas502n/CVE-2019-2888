package weblogic.servlet.ejb2jsp.dd;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import weblogic.servlet.ejb2jsp.BeanGenerator;
import weblogic.servlet.ejb2jsp.EJBMethodGenerator;
import weblogic.servlet.ejb2jsp.EJBTaglibGenerator;
import weblogic.servlet.ejb2jsp.HomeCollectionGenerator;
import weblogic.servlet.ejb2jsp.HomeFinderGenerator;
import weblogic.servlet.ejb2jsp.HomeMethodGenerator;
import weblogic.servlet.internal.dd.ToXML;
import weblogic.utils.Getopt2;
import weblogic.utils.classloaders.ClasspathClassLoader;
import weblogic.utils.io.XMLWriter;
import weblogic.xml.dom.DOMProcessingException;
import weblogic.xml.dom.DOMUtils;
import weblogic.xml.jaxp.WebLogicDocumentBuilderFactory;

public class EJBTaglibDescriptor implements ToXML, Externalizable {
   private static final long serialVersionUID = -9016538269900747655L;
   private FilesystemInfoDescriptor fileInfo;
   private BeanDescriptor[] beans;
   private transient ClassLoader jarLoader;
   private static final String PREAMBLE = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n<!DOCTYPE ejb2jsp-taglib PUBLIC \"-//BEA Systems, Inc.//DTD EJB2JSP Taglib 1.0//EN\" \"http://www.bea.com/servers/wls600/dtd/weblogic-ejb2jsp.dtd\">";

   static void p(String var0) {
      System.err.println("[EJBTagDesc]: " + var0);
   }

   public EJBTaglibDescriptor() {
      this.fileInfo = new FilesystemInfoDescriptor();
      this.beans = new BeanDescriptor[0];
   }

   public EJBTaglibDescriptor(Element var1) throws DOMProcessingException {
      this.initFromRoot(var1);
   }

   private void initFromRoot(Element var1) throws DOMProcessingException {
      Element var2 = null;
      var2 = DOMUtils.getElementByTagName(var1, "filesystem-info");
      this.fileInfo = new FilesystemInfoDescriptor(var2);
      List var3 = DOMUtils.getElementsByTagName(var1, "ejb");
      ArrayList var4 = new ArrayList();
      Iterator var5 = var3.iterator();

      while(var5.hasNext()) {
         var4.add(new BeanDescriptor((Element)var5.next()));
      }

      this.beans = new BeanDescriptor[var4.size()];
      var4.toArray(this.beans);
   }

   public String toString() {
      String var1 = this.getFileInfo().getEJBJarFile();
      int var2 = var1.lastIndexOf(47);
      int var3 = var1.lastIndexOf(File.separatorChar);
      int var4 = Math.max(var2, var3);
      return var4 < 0 ? var1 : var1.substring(var4 + 1);
   }

   public void writeExternal(ObjectOutput var1) throws IOException {
      var1.writeUTF(toString(this));
   }

   public void readExternal(ObjectInput var1) throws IOException, ClassNotFoundException {
      StringReader var2 = new StringReader(var1.readUTF());

      try {
         load(var2, this);
      } catch (RuntimeException var4) {
         throw var4;
      } catch (IOException var5) {
         throw var5;
      } catch (Exception var6) {
         throw new IOException("error reading XML: " + var6);
      }
   }

   public ClassLoader getClassLoader() {
      if (this.jarLoader == null) {
         this.jarLoader = new ClasspathClassLoader(this.getFileInfo().getEJBJarFile());
      }

      return this.jarLoader;
   }

   public FilesystemInfoDescriptor getFileInfo() {
      return this.fileInfo;
   }

   public void setFileInfo(FilesystemInfoDescriptor var1) {
      this.fileInfo = var1;
   }

   public BeanDescriptor[] getBeans() {
      if (this.beans == null) {
         this.beans = new BeanDescriptor[0];
      }

      return (BeanDescriptor[])((BeanDescriptor[])this.beans.clone());
   }

   public void setBeans(BeanDescriptor[] var1) {
      if (var1 == null) {
         this.beans = new BeanDescriptor[0];
      } else {
         this.beans = (BeanDescriptor[])((BeanDescriptor[])var1.clone());
      }

   }

   public void setEnableBaseEJB(boolean var1) {
      BeanDescriptor[] var2 = this.getBeans();

      for(int var3 = 0; var3 < var2.length; ++var3) {
         var2[var3].setEnableBaseEJB(var1);
      }

   }

   public Getopt2 getOpts() throws Exception {
      Getopt2 var1 = new Getopt2();
      var1.addOption("d", "destination directory", "working dir of codegen");
      String var2 = null;
      if (this.getFileInfo().saveAsDirectory()) {
         var2 = this.getFileInfo().getSaveDirClassDir();
      } else {
         var2 = this.getFileInfo().getSaveJarTmpdir();
      }

      var1.setOption("d", var2);
      return var1;
   }

   public EJBTaglibGenerator createGenerator() throws Exception {
      if (!this.getFileInfo().saveAsDirectory()) {
         String var1 = this.getFileInfo().getSaveJarTmpdir();
         File var2 = new File(var1.replace('/', File.separatorChar));
         if (!var2.mkdirs() && !var2.isDirectory()) {
            throw new Exception("cannot make tmp directory '" + var2.getAbsolutePath() + "'");
         }
      }

      EJBTaglibGenerator var6 = new EJBTaglibGenerator(this);
      int var7 = 0;

      for(int var3 = 0; var3 < this.beans.length; ++var3) {
         if (this.beans[var3].isEnabled()) {
            ++var7;
         }
      }

      BeanGenerator[] var8 = new BeanGenerator[var7];
      int var4 = 0;

      for(int var5 = 0; var5 < this.beans.length; ++var5) {
         if (this.beans[var5].isEnabled()) {
            var8[var4++] = this.addMethodGenerators(this.beans[var5], var6);
         }
      }

      var6.setGenerators(var8);
      return var6;
   }

   private BeanGenerator addMethodGenerators(BeanDescriptor var1, EJBTaglibGenerator var2) throws Exception {
      EJBMethodDescriptor[] var3 = var1.getEJBMethods();
      BeanGenerator var4 = new BeanGenerator(this, var1);
      Getopt2 var5 = this.getOpts();

      for(int var6 = 0; var6 < var3.length; ++var6) {
         if (var3[var6].isEnabled()) {
            EJBMethodGenerator var7 = new EJBMethodGenerator(var5, var4, var3[var6]);
            var4.addMethod(var7);
         }
      }

      if (var1.isStatefulBean()) {
         var3 = var1.getHomeMethods();
         Object var12 = null;

         for(int var13 = 0; var13 < var3.length; ++var13) {
            if (var3[var13].isEnabled()) {
               String var8 = var3[var13].getName();
               if (!var8.startsWith("find") && !var8.startsWith("create")) {
                  var12 = new HomeMethodGenerator(var5, var4, var3[var13]);
               } else {
                  Class var9 = this.getClassLoader().loadClass(var3[var13].getReturnType());
                  if (var9.getName().equals(var1.getRemoteType())) {
                     var12 = new HomeFinderGenerator(var5, var4, var3[var13]);
                  } else {
                     Class var10 = Collection.class;
                     Class var11 = Enumeration.class;
                     if (var10.isAssignableFrom(var9)) {
                        var12 = new HomeCollectionGenerator(var5, var4, var3[var13]);
                     } else {
                        if (!var11.isAssignableFrom(var9)) {
                           throw new IllegalArgumentException("illegal finder on home: " + var3[var13].getSignature());
                        }

                        var12 = new HomeCollectionGenerator(var5, var4, var3[var13]);
                        ((HomeCollectionGenerator)var12).setIsEnumeration(true);
                     }
                  }
               }

               if (var12 == null) {
                  throw new Error("no generator type for " + var3[var13].getSignature());
               }

               var4.addMethod((EJBMethodGenerator)var12);
            }
         }
      }

      return var4;
   }

   public String[] getErrors() {
      ArrayList var1 = new ArrayList();
      String[] var2 = this.getFileInfo().getErrors();

      for(int var3 = 0; var2 != null && var3 < var2.length; ++var3) {
         var1.add(var2[var3]);
      }

      this.getDuplicateTagNames(var1);
      this.getDuplicateAttributeNames(var1);
      this.getUnresolvedMethods(var1);
      String[] var4 = new String[var1.size()];
      var1.toArray(var4);
      return var4;
   }

   private void getDuplicateAttributeNames(List var1) {
      for(int var2 = 0; this.beans != null && var2 < this.beans.length; ++var2) {
         this.beans[var2].getDuplicateAttributeNames(var1);
      }

   }

   private void getUnresolvedMethods(List var1) {
      for(int var2 = 0; this.beans != null && var2 < this.beans.length; ++var2) {
         if (this.beans[var2].isEnabled()) {
            EJBMethodDescriptor[] var3 = this.beans[var2].getUnresolvedMethods();

            for(int var4 = 0; var3 != null && var4 < var3.length; ++var4) {
               String var5 = "tag " + var3[var4].getTagName() + " for method " + var3[var4].getName() + " on " + var3[var4].getTargetType() + " appears to " + "have meaningless parameter names (arg0,arg1,....)";
               var1.add(var5);
            }
         }
      }

   }

   private void getDuplicateTagNames(List var1) {
      ArrayList var2 = new ArrayList();

      for(int var3 = 0; this.beans != null && var3 < this.beans.length; ++var3) {
         EJBMethodDescriptor[] var4 = this.beans[var3].getEJBMethods();

         int var5;
         for(var5 = 0; var4 != null && var5 < var4.length; ++var5) {
            if (var4[var5].isEnabled()) {
               var2.add(var4[var5]);
            }
         }

         var4 = this.beans[var3].getHomeMethods();

         for(var5 = 0; var4 != null && var5 < var4.length; ++var5) {
            if (var4[var5].isEnabled()) {
               var2.add(var4[var5]);
            }
         }
      }

      EJBMethodDescriptor[] var6 = new EJBMethodDescriptor[var2.size()];
      var2.toArray(var6);
      this.getDuplicateTagNames(var1, var6);
   }

   private void getDuplicateTagNames(List var1, EJBMethodDescriptor[] var2) {
      if (var2 != null) {
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3 - 1; ++var4) {
            String var5 = var2[var4].getTagName();

            for(int var6 = var4 + 1; var6 < var3; ++var6) {
               if (var5 != null && var5.equals(var2[var6].getTagName())) {
                  String var7 = "duplicate tag names \"" + var5 + "\" refer to methods \"" + var2[var4].getSignature() + "\" and \"" + var2[var6].getSignature() + "\"";
                  var1.add(var7);
               }
            }
         }

      }
   }

   public static EJBTaglibDescriptor load(File var0) throws Exception {
      InputStreamReader var1 = new InputStreamReader(new FileInputStream(var0));

      EJBTaglibDescriptor var2;
      try {
         var2 = load((Reader)var1);
      } finally {
         var1.close();
      }

      return var2;
   }

   public static String toString(EJBTaglibDescriptor var0) {
      StringWriter var1 = new StringWriter();
      XMLWriter var2 = new XMLWriter(var1);
      var0.toXML(var2);
      var2.flush();
      return var1.toString();
   }

   public static EJBTaglibDescriptor load(String var0) throws Exception {
      return load((Reader)(new StringReader(var0)));
   }

   public static EJBTaglibDescriptor load(Reader var0) throws Exception {
      return load(var0, new EJBTaglibDescriptor());
   }

   private static EJBTaglibDescriptor load(Reader var0, EJBTaglibDescriptor var1) throws Exception {
      WebLogicDocumentBuilderFactory var2 = new WebLogicDocumentBuilderFactory();
      var2.setValidating(true);
      DocumentBuilder var3 = var2.newDocumentBuilder();
      var3.setEntityResolver(new EJBTaglibEntityResolver());
      InputSource var4 = new InputSource(var0);
      Element var5 = null;
      var5 = var3.parse(var4).getDocumentElement();
      var1.initFromRoot(var5);
      return var1;
   }

   public static void main(String[] var0) throws Exception {
      File var1 = new File(var0[0]);
      EJBTaglibDescriptor var2 = load(var1);
      XMLWriter var3 = new XMLWriter(System.out);
      var2.toXML(var3);
      var3.flush();
   }

   public void toXML(XMLWriter var1) {
      var1.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n<!DOCTYPE ejb2jsp-taglib PUBLIC \"-//BEA Systems, Inc.//DTD EJB2JSP Taglib 1.0//EN\" \"http://www.bea.com/servers/wls600/dtd/weblogic-ejb2jsp.dtd\">");
      var1.println("<ejb2jsp-taglib>");
      var1.incrIndent();
      this.fileInfo.toXML(var1);
      if (this.beans != null && this.beans.length != 0) {
         for(int var2 = 0; this.beans != null && var2 < this.beans.length; ++var2) {
            this.beans[var2].toXML(var1);
         }

         var1.decrIndent();
         var1.println("</ejb2jsp-taglib>");
      } else {
         throw new IllegalStateException("cannot save xml descriptor file with no bean entries");
      }
   }
}
