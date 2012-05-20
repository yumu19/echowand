package echowand.net;

import echowand.net.StandardPayloadValidator;
import echowand.net.Property;
import echowand.net.StandardPayload;
import echowand.common.Data;
import echowand.common.ESV;
import echowand.common.EOJ;
import echowand.common.EPC;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Yoshiki Makino
 */
public class StandardPayloadValidatorTest {
    
    @Test
    public void testSimpleValidationFail() {
        StandardPayloadValidator validator = new StandardPayloadValidator();
        StandardPayload payload = new StandardPayload();
        assertFalse(validator.validate(payload));
        assertFalse(validator.validateSEOJ(payload));
        assertFalse(validator.validateDEOJ(payload));
        assertFalse(validator.validateESV(payload));
        assertFalse(validator.validateProperties(payload));
    }
    
    @Test
    public void testGetValidation() {
        StandardPayloadValidator validator = new StandardPayloadValidator();
        StandardPayload payload = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.Get);
        payload.addFirstProperty(new Property(EPC.x80));
        assertTrue(validator.validate(payload));
        assertTrue(validator.validateSEOJ(payload));
        assertTrue(validator.validateDEOJ(payload));
        assertTrue(validator.validateESV(payload));
        assertTrue(validator.validateProperties(payload));
        
        payload.addFirstProperty(new Property(EPC.x80, new Data((byte)0x12)));
        assertFalse(validator.validate(payload));
        assertTrue(validator.validateSEOJ(payload));
        assertTrue(validator.validateDEOJ(payload));
        assertTrue(validator.validateESV(payload));
        assertFalse(validator.validateProperties(payload));
    }
    
    @Test
    public void testSetValidation() {
        StandardPayloadValidator validator = new StandardPayloadValidator();
        StandardPayload payload = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.SetI);
        payload.addFirstProperty(new Property(EPC.x80, new Data((byte)0x12)));
        assertTrue(validator.validate(payload));
        assertTrue(validator.validateSEOJ(payload));
        assertTrue(validator.validateDEOJ(payload));
        assertTrue(validator.validateESV(payload));
        assertTrue(validator.validateProperties(payload));
        
        payload.addFirstProperty(new Property(EPC.x80));
        assertFalse(validator.validate(payload));
        assertFalse(validator.validateProperties(payload));
    }
    
    @Test
    public void testInvalidEPC() {
        StandardPayloadValidator validator = new StandardPayloadValidator();
        StandardPayload payload = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.SetC);
        payload.addFirstProperty(new Property(EPC.Invalid, new Data((byte)0x12)));
        assertFalse(validator.validate(payload));
        assertTrue(validator.validateSEOJ(payload));
        assertTrue(validator.validateDEOJ(payload));
        assertTrue(validator.validateESV(payload));
        assertFalse(validator.validateProperties(payload));
    }
    
    @Test
    public void testInvalidDataSize() {
        StandardPayloadValidator validator = new StandardPayloadValidator();
        StandardPayload payload1 = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.SetC);
        payload1.addFirstProperty(new Property(EPC.xE0, new Data(new byte[257])));
        assertFalse(validator.validate(payload1));
        assertTrue(validator.validateSEOJ(payload1));
        assertTrue(validator.validateDEOJ(payload1));
        assertTrue(validator.validateESV(payload1));
        assertFalse(validator.validateProperties(payload1));
        
        StandardPayload payload2 = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.SetC);
        payload2.addFirstProperty(new Property(EPC.xE0, new Data(new byte[256])));
        assertFalse(validator.validate(payload2));
        assertTrue(validator.validateSEOJ(payload2));
        assertTrue(validator.validateDEOJ(payload2));
        assertTrue(validator.validateESV(payload2));
        assertFalse(validator.validateProperties(payload2));
        
        StandardPayload payload3 = new StandardPayload(new EOJ("001101"), new EOJ("001101"), ESV.SetC);
        payload3.addFirstProperty(new Property(EPC.xE0, new Data()));
        assertFalse(validator.validate(payload3));
        assertTrue(validator.validateSEOJ(payload3));
        assertTrue(validator.validateDEOJ(payload3));
        assertTrue(validator.validateESV(payload3));
        assertFalse(validator.validateProperties(payload3));
    }
}
