package ch.cern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;

import ch.cern.Properties.PropertiesCache;

public class PropertiesTest {
    
    @Test
    public void globalParametersNull(){
        Properties prop = new Properties();
        prop.setProperty("prop1", "val1");
        prop.setProperty("prop2.prop1", "val2");
        
        Properties subProp = prop.getSubset("prop2");
        
        Assert.assertEquals(1, subProp.size());
        Assert.assertEquals("val2", subProp.get("prop1"));
    }

	@Test
	public void expiration() throws Exception{
		Properties.PropertiesCache prop = new Properties.PropertiesCache("src/test/resources/config.properties", Duration.ofSeconds(1));
		
		Properties p1 = prop.get();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Properties p2 = prop.get();
		
		Assert.assertSame(p1, p2);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Properties p3 = prop.get();
		
		Assert.assertNotSame(p1, p3);
	}
	
	@Test
	public void propertiesFromSource() throws Exception{
		Properties.PropertiesCache prop = new Properties.PropertiesCache("src/test/resources/config.properties", Duration.ofSeconds(1));

		assertNotEquals("not-valid-already-declared", prop.get().getProperty("source.type"));
		assertNotEquals("not-valid-already-declared", prop.get().getProperty("results.sink.type"));
		
		assertEquals("val1", prop.get().getProperty("key1"));
		assertEquals("val2", prop.get().getProperty("key2"));
	}
	
	@Test
	public void getUniqueKeyFields() {
		Properties prop = new Properties();
        prop.setProperty("prop1", "val1");
        prop.setProperty("prop2.prop21", "val2");
        prop.setProperty("prop2.prop22", "val2");
        prop.setProperty("prop2.prop23", "val2");
        prop.setProperty("prop3.prop31", "val2");
        prop.setProperty("prop3.prop32", "val2");
        
        Object[] uniq = prop.getUniqueKeyFields().toArray();
        String[] expectedValue = {"prop2", "prop1", "prop3"};
        Assert.assertArrayEquals(expectedValue, uniq);
	}

    public static PropertiesCache mockedExpirable() {
        PropertiesCache propExp = mock(PropertiesCache.class, withSettings().serializable());
        
        try {
            when(propExp.get()).thenReturn(new Properties());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return propExp;
    }
	
}