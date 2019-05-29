package de.edirom.meigarage.mei;

import net.sf.saxon.s9api.*;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.tei.utils.SaxonProcFactory;
import org.tei.utils.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import pl.psnc.dl.ege.component.Customization;
import pl.psnc.dl.ege.configuration.EGEConfigurationManager;
import pl.psnc.dl.ege.configuration.EGEConstants;
import pl.psnc.dl.ege.exception.ConverterException;
import pl.psnc.dl.ege.exception.CustomizationException;
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
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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

        sources.add(new CustomizationSourceInputType("mei401", "MEI v4.0.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei300", "MEI v3.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei211", "MEI v2.1.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei200", "MEI v2.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
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
        LOGGER.info("Error: " + exception.getMessage());
    }


    public void fatalError(SAXParseException exception) throws SAXException {
        LOGGER.info("Fatal Error: " + exception.getMessage());
        throw exception;
    }


    public void warning(SAXParseException exception) throws SAXException {
        LOGGER.info("Warning: " + exception.getMessage());
    }

    public void customize(CustomizationSetting customizationSetting, String sourceId, String customizationId, String outputFormat, OutputStream outputStream) throws IOException, EGEException {
        LOGGER.trace("MEICustomization.customize()");

        if(outputFormat == "RelaxNG") {

            String sourcePath = MEI_DIR + File.separator + "mei401/source/mei-source.xml";
            String customizationPath = MEI_DIR + File.separator + "mei401/customizations/mei-all.xml";

            performCustomization(sourcePath, customizationPath, outputStream);
        }
    }

    /*
     * Performs transformation with XSLT
     */
    private void performCustomization(String source, String customization,
                                           OutputStream outputStream)
            throws IOException, CustomizationException {


        File outTempDir = null;
        FileInputStream is = null;
        try {
            outTempDir = prepareTempDir();
            File buildFile = new File(TEI_DIR + "/relaxng/build-to.xml");

            Project p = new Project();
            p.setUserProperty("ant.file", buildFile.getAbsolutePath());
            p.setProperty("inputFile", customization);
            p.setProperty("outputFile", outTempDir.getAbsolutePath() + "/customization_.rng");
            p.setProperty("defaultSource", source);

            p.init();

            ProjectHelper helper = ProjectHelper.getProjectHelper();
            p.addReference("ant.projectHelper", helper);

            helper.parse(p, buildFile);

            p.executeTarget(p.getDefaultTarget());

            File relax = new File(outTempDir.getAbsolutePath() + "/customization_.rng");
            is = new FileInputStream(relax);

            byte[] buf = new byte[8192];
            int c = 0;
            while ((c = is.read(buf, 0, buf.length)) > 0) {
                outputStream.write(buf, 0, c);
            }

        }catch (Exception e) {
            throw new CustomizationException();
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
            if (outTempDir != null && outTempDir.exists())
                EGEIOUtils.deleteDirectory(outTempDir);
        }
    }

    private File prepareTempDir() {
        File inTempDir = null;
        String uid = UUID.randomUUID().toString();
        inTempDir = new File(EGEConstants.TEMP_PATH + File.separator + uid
                + File.separator);
        inTempDir.mkdir();
        return inTempDir;
    }

    public List<CustomizationSetting> getSupportedCustomizationSettings() {
        List<CustomizationSetting> css = new ArrayList<CustomizationSetting>();
        css.add(cs);
        return css;
    }
}
