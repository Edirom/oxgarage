package de.edirom.meigarage.mei;

import net.sf.saxon.s9api.*;
import org.apache.log4j.Logger;
import org.tei.utils.SaxonProcFactory;
import org.tei.utils.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import pl.psnc.dl.ege.component.Customization;
import pl.psnc.dl.ege.configuration.EGEConfigurationManager;
import pl.psnc.dl.ege.configuration.EGEConstants;
import pl.psnc.dl.ege.exception.EGEException;
import pl.psnc.dl.ege.types.CustomizationSetting;
import pl.psnc.dl.ege.types.CustomizationSourceInputType;
import pl.psnc.dl.ege.utils.EGEIOUtils;
import pl.psnc.dl.ege.utils.IOResolver;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MEICustomization implements Customization, ErrorHandler {


    private static final Logger LOGGER = Logger.getLogger(MEICustomization.class);

    private static final CustomizationSetting cs;

    private static final String TEI_DIR;
    private static final String MEI_DIR = "/usr/share/xml/mei/music-encoding";

    static {
        TEI_DIR = EGEConstants.TEIROOT + "stylesheet" ;

        List<CustomizationSourceInputType> sources = new ArrayList<CustomizationSourceInputType>();
        List<CustomizationSourceInputType> customizations = new ArrayList<CustomizationSourceInputType>();
        List<String> outputFormats = new ArrayList<String>();

        sources.add(new CustomizationSourceInputType("mei401", "MEI v4.0.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "source/mei-source_canon.xml"));
        sources.add(new CustomizationSourceInputType("mei300", "MEI v3.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "source/mei-source_canon.xml"));
        sources.add(new CustomizationSourceInputType("mei211", "MEI v2.1.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "source/mei-source_canon.xml"));
        sources.add(new CustomizationSourceInputType("mei200", "MEI v2.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "source/mei-source_canon.xml"));
        sources.add(new CustomizationSourceInputType("mei-local", "Local Source", CustomizationSourceInputType.TYPE_CLIENT_FILE));

        customizations.add(new CustomizationSourceInputType("c-mei-all", "MEI All", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-all.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-all-any", "MEI All anyStart", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-all_anyStart.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-cmn", "MEI CMN", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-CMN.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-mensural", "MEI Mensural", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-Mensural.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-neumes", "MEI Neumes", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-Neumes.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-local", "Local Customization", CustomizationSourceInputType.TYPE_CLIENT_FILE));

        outputFormats.add("RelaxNG");
        outputFormats.add("Compiled ODD");

        cs = new CustomizationSetting("mei", sources, customizations, outputFormats);
    }

    private IOResolver ior = EGEConfigurationManager.getInstance()
            .getStandardIOResolver();

    public void error(SAXParseException exception) throws SAXException {
        LOGGER.error("Error: " + exception.getMessage());
    }


    public void fatalError(SAXParseException exception) throws SAXException {
        LOGGER.error("Fatal Error: " + exception.getMessage());
        throw exception;
    }


    public void warning(SAXParseException exception) throws SAXException {
        LOGGER.error("Warning: " + exception.getMessage());
    }

    public void customize(CustomizationSetting customizationSetting, CustomizationSourceInputType sourceInputType,
                          CustomizationSourceInputType customizationInputType, String outputFormat,
                          OutputStream outputStream, File localSourceFile, File localCustomizationFile) throws EGEException {

        File outTempDir = prepareTempDir();

        String sourcePath = "";
        if(sourceInputType.getType().equals(CustomizationSourceInputType.TYPE_CLIENT_FILE)) {

            sourcePath = localSourceFile.getAbsolutePath();
        }else
            sourcePath = MEI_DIR + File.separator + sourceInputType.getId() + File.separator + sourceInputType.getPath();

        String customizationPath = "";
        if(customizationInputType.getType().equals(CustomizationSourceInputType.TYPE_CLIENT_FILE)) {
            customizationPath = localCustomizationFile.getAbsolutePath();
        }else
            customizationPath = MEI_DIR + File.separator + sourceInputType.getId() + File.separator + customizationInputType.getPath();


        performCustomization(outputFormat, sourcePath, customizationPath, customizationInputType.getId(), outputStream, outTempDir);

        if (outTempDir != null && outTempDir.exists())
            EGEIOUtils.deleteDirectory(outTempDir);

    }

    /*
     * Performs transformation with XSLT
     */
    private void performCustomization(String outputFormat, String sourcePath, String customizationPath,
                                      String customizationName, OutputStream outputStream, File outTempDir)
            throws EGEException {


        LOGGER.debug("performCustomization(" + sourcePath + ", " + customizationPath + ")");

        FileInputStream is = null;


        try {
/*            // Prepare Source File
            LOGGER.debug("prepare source file: " + sourcePath +
                    " -> " + outTempDir.getAbsolutePath() + File.separator +  "source.xml");

            File sourceFile = prepareSourceFile(sourcePath, outTempDir);
*/

            if(outputFormat.equals("RelaxNG")) {
                // Expand ODD
                LOGGER.debug("expand odd: " + customizationPath + " through " + TEI_DIR + "/odds/odd2odd.xsl" +
                        " with source " + sourcePath +
                        " -> " + outTempDir.getAbsolutePath() + File.separator +  "processedodd.xml");

                File processedOddFile = expandODD(customizationPath, outTempDir, sourcePath);

                // Build RelaxNG
                LOGGER.debug("generate rng: " + processedOddFile.getAbsolutePath() + " through " + TEI_DIR + "/odds/odd2relax.xsl" +
                        " with source " + sourcePath +
                        " -> " + outTempDir.getAbsolutePath() + File.separator +  customizationName + ".rng");

                File relaxNGFile = transformToRelaxNG(processedOddFile, outTempDir, customizationName, sourcePath);

                is = new FileInputStream(relaxNGFile);

            }else if(outputFormat.equals("Compiled ODD")) {
                // Expand ODD
                LOGGER.debug("expand odd: " + customizationPath + " through " + TEI_DIR + "/odds/odd2odd.xsl" +
                        " with source " + sourcePath +
                        " -> " + outTempDir.getAbsolutePath() + File.separator +  customizationName +  ".xml");

                File processedOddFile = expandODD(customizationPath, outTempDir, sourcePath);

                is = new FileInputStream(processedOddFile);
            }

            byte[] buf = new byte[8192];
            int c = 0;
            while ((c = is.read(buf, 0, buf.length)) > 0) {
                outputStream.write(buf, 0, c);
            }

        }catch (Exception e) {
            throw new EGEException(e.getMessage());
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
                // do nothing
            }
            try {
                outputStream.flush();
                outputStream.close();
            } catch (Exception ex) {
                // do nothing
            }
        }
    }

/*    private File prepareSourceFile(String sourcePath, File outTempDir) throws SaxonApiException, IOException, SAXException {

        PrintWriter out = new PrintWriter(outTempDir + "resolveXInclude.xsl");
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "    exclude-result-prefixes=\"xs\"\n" +
                "    version=\"2.0\">\n" +
                "    \n" +
                "    <xsl:output encoding=\"UTF-8\" indent=\"yes\"/>\n" +
                "    \n" +
                "    <xsl:template match=\"/\">\n" +
                "        <xsl:apply-templates/>\n" +
                "    </xsl:template>\n" +
                "    \n" +
                "    <xsl:template match=\"@*\">\n" +
                "        <xsl:copy-of select=\".\"/>\n" +
                "    </xsl:template>\n" +
                "    \n" +
                "    <xsl:template match=\"*\">\n" +
                "        <xsl:copy>\n" +
                "            <xsl:apply-templates select=\"@* | element()\"/>\n" +
                "        </xsl:copy>\n" +
                "    </xsl:template>\n" +
                "    \n" +
                "</xsl:stylesheet>");
        out.flush();
        out.close();

        Processor proc = SaxonProcFactory.getProcessor();
        XsltCompiler comp = proc.newXsltCompiler();

        StreamSource xslt = new StreamSource(new FileInputStream(new File(
                outTempDir + "resolveXInclude.xsl")));

        XsltExecutable exec = comp.compile(xslt);
        Xslt30Transformer transformer = exec.load30();

        ParseOptions options = new ParseOptions();
        options.setXIncludeAware(true);
        XMLReader reader = options.getXMLReader();

        File sourceFile = new File(outTempDir.getAbsolutePath() + File.separator +  "source.xml");
        Serializer s = transformer.newSerializer(sourceFile);
        transformer.applyTemplates(new SAXSource(reader, new InputSource(sourcePath)), new XMLStreamWriterDestination(s.getXMLStreamWriter()));

        return sourceFile;
    }
 */

    private File transformToRelaxNG(File processedOddFile, File outTempDir, String customizationName, String sourcePath) throws FileNotFoundException, SaxonApiException {
        Processor proc = SaxonProcFactory.getProcessor();
        XsltCompiler comp = proc.newXsltCompiler();

        StreamSource xslt = new StreamSource(new FileInputStream(new File(
                TEI_DIR + "/odds/odd2relax.xsl")));
        xslt.setSystemId(TEI_DIR + "/odds/odd2relax.xsl");
        XsltExecutable exec = comp.compile(xslt);
        XsltTransformer transformer = exec.load();

        transformer.setSource(new StreamSource(processedOddFile));

        File relaxNGFile = new File(outTempDir.getAbsolutePath() + File.separator +  customizationName + ".rng");
        Serializer relaxNGSerializer = proc.newSerializer(relaxNGFile);
        transformer.setDestination(relaxNGSerializer);

        transformer.setParameter(new QName("defaultSource"), XdmValue.makeValue(sourcePath));
        transformer.setParameter(new QName("default-ns"), XdmValue.makeValue("http://www.music-encoding.org/ns/mei"));
        transformer.setParameter(new QName("prefixes"), XdmValue.makeValue("mei=http://www.music-encoding.org/ns/mei tei=http://www.tei-c.org/ns/1.0 teix=http://www.tei-c.org/ns/Examples rng=http://relaxng.org/ns/structure/1.0"));
        transformer.transform();

        return relaxNGFile;
    }

    private File expandODD(String customizationPath, File outTempDir, String sourcePath) throws FileNotFoundException, SaxonApiException {
        Processor proc = SaxonProcFactory.getProcessor();
        XsltCompiler comp = proc.newXsltCompiler();

        StreamSource xslt = new StreamSource(new FileInputStream(new File(
                TEI_DIR + "/odds/odd2odd.xsl")));
        xslt.setSystemId(TEI_DIR + "/odds/odd2odd.xsl");
        XsltExecutable exec = comp.compile(xslt);
        XsltTransformer transformer = exec.load();

        File inputFile = new File(customizationPath);
        transformer.setSource(new StreamSource(inputFile));

        File processedOddFile = new File(outTempDir.getAbsolutePath() + File.separator +  "processedodd.xml");
        Serializer processedOddSerializer = proc.newSerializer(processedOddFile);
        transformer.setDestination(processedOddSerializer);

        transformer.setParameter(new QName("defaultSource"), XdmValue.makeValue(sourcePath));
        transformer.transform();

        return processedOddFile;
    }

    private File prepareTempDir() {
        File inTempDir = null;
        String uid = UUID.randomUUID().toString();
        inTempDir = new File(EGEConstants.TEMP_PATH + File.separator + uid
                + File.separator);
        inTempDir.mkdir();
        LOGGER.info("Temp dir created: " + inTempDir.getAbsolutePath());
        return inTempDir;
    }

    public List<CustomizationSetting> getSupportedCustomizationSettings() {
        List<CustomizationSetting> css = new ArrayList<CustomizationSetting>();
        css.add(cs);
        return css;
    }
}
