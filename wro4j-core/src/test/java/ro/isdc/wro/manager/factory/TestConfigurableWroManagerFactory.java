/*
 * Copyright (c) 2010. All rights reserved.
 */
package ro.isdc.wro.manager.factory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.manager.WroManager;
import ro.isdc.wro.model.resource.locator.ClasspathUriLocator;
import ro.isdc.wro.model.resource.locator.ServletContextUriLocator;
import ro.isdc.wro.model.resource.locator.UriLocator;
import ro.isdc.wro.model.resource.processor.ResourceProcessor;
import ro.isdc.wro.model.resource.processor.decorator.ExtensionsAwareProcessorDecorator;
import ro.isdc.wro.model.resource.processor.factory.ConfigurableProcessorsFactory;
import ro.isdc.wro.model.resource.processor.factory.ProcessorsFactory;
import ro.isdc.wro.model.resource.processor.impl.css.CssImportPreProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssMinProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssVariablesProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;
import ro.isdc.wro.model.resource.support.hash.ConfigurableHashStrategy;
import ro.isdc.wro.model.resource.support.hash.HashStrategy;
import ro.isdc.wro.model.resource.support.hash.MD5HashStrategy;
import ro.isdc.wro.model.resource.support.naming.ConfigurableNamingStrategy;
import ro.isdc.wro.model.resource.support.naming.NamingStrategy;
import ro.isdc.wro.model.resource.support.naming.TimestampNamingStrategy;
import ro.isdc.wro.util.WroUtil;


/**
 * TestConfigurableWroManagerFactory.
 * 
 * @author Alex Objelean
 * @created Created on Jan 5, 2010
 */
public class TestConfigurableWroManagerFactory {
  private ConfigurableWroManagerFactory victim;
  @Mock
  private FilterConfig mockFilterConfig;
  @Mock
  private ServletContext mockServletContext;
  private ProcessorsFactory processorsFactory;
  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  private Properties configProperties;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    // init context
    Context.set(Context.webContext(mockRequest, mockResponse, mockFilterConfig));
    Mockito.when(mockFilterConfig.getServletContext()).thenReturn(mockServletContext);
    victim = new ConfigurableWroManagerFactory();
    configProperties = new Properties();
    victim.setConfigProperties(configProperties);
  }

  /**
   * Creaes the manager and initialize processors with locators used for assetions.
   */
  private void createManager() {
    // create one instance for test
    final WroManager manager = victim.create();
    processorsFactory = manager.getProcessorsFactory();
    uriLocatorFactory = (SimpleUriLocatorFactory) AbstractDecorator.getOriginalDecoratedObject(manager.getUriLocatorFactory());
  }
  
  @After
  public void tearDown() {
    Context.unset();
  }
  
  /**
   * When no uri locators are set, the default factory is used.
   */
  @Test
  public void testWhenNoUriLocatorsParamSet() {
    createManager();
    Assert.assertFalse(uriLocatorFactory.getUriLocators().isEmpty());
  }
  
  @Test
  public void testWithEmptyUriLocators() {
    createManager();
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS)).thenReturn("");
    Assert.assertFalse(uriLocatorFactory.getUriLocators().isEmpty());
  }
  
  
  @Test
  public void shouldLoadUriLocatorsFromConfigurationFile() {
    configProperties.setProperty(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS, "servletContext");
    
    createManager();
    
    Assert.assertEquals(1, uriLocatorFactory.getUriLocators().size());
    Assert.assertSame(ServletContextUriLocator.class, uriLocatorFactory.getUriLocators().iterator().next().getClass());
  }
  
  @Test
  public void shouldLoadUriLocatorsFromFilterConfigRatherThanFromConfigProperties() {
    configProperties.setProperty(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS, "servletContext");
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS)).thenReturn("classpath, servletContext");
    
    createManager();
    
    Assert.assertEquals(2, uriLocatorFactory.getUriLocators().size());
    final Iterator<UriLocator> locatorsIterator = uriLocatorFactory.getUriLocators().iterator();
    Assert.assertSame(ClasspathUriLocator.class, locatorsIterator.next().getClass());
    Assert.assertSame(ServletContextUriLocator.class, locatorsIterator.next().getClass());
  }

  
  @Test(expected = WroRuntimeException.class)
  public void cannotUseInvalidUriLocatorsSet() {
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS)).thenReturn(
        "INVALID1,INVALID2");
    
    createManager();
    
    uriLocatorFactory.getUriLocators();
  }
  
  @Test
  public void testWhenValidLocatorsSet() {
    createManager();
    
    configureValidUriLocators(mockFilterConfig);
    Assert.assertEquals(3, uriLocatorFactory.getUriLocators().size());
  }
  
  /**
   * @param filterConfig
   */
  private void configureValidUriLocators(final FilterConfig filterConfig) {
    Mockito.when(filterConfig.getInitParameter(ConfigurableWroManagerFactory.PARAM_URI_LOCATORS)).thenReturn(
        "servletContext, url, classpath");
  }
  
  @Test
  public void testProcessorsExecutionOrder() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS)).thenReturn(
        JSMinProcessor.ALIAS + "," + CssImportPreProcessor.ALIAS + "," + CssVariablesProcessor.ALIAS);
    final List<ResourceProcessor> list = (List<ResourceProcessor>) processorsFactory.getPreProcessors();
    Assert.assertEquals(JSMinProcessor.class, list.get(0).getClass());
    Assert.assertEquals(CssImportPreProcessor.class, list.get(1).getClass());
    Assert.assertEquals(CssVariablesProcessor.class, list.get(2).getClass());
  }
  
  @Test
  public void testWithEmptyPreProcessors() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS)).thenReturn("");
    Assert.assertTrue(processorsFactory.getPreProcessors().isEmpty());
  }
  
  @Test(expected = WroRuntimeException.class)
  public void cannotUseInvalidPreProcessorsSet() {
    createManager();
    
    configureValidUriLocators(mockFilterConfig);
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS)).thenReturn(
        "INVALID1,INVALID2");
    processorsFactory.getPreProcessors();
  }
  
  @Test
  public void testWhenValidPreProcessorsSet() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS)).thenReturn(
        "cssUrlRewriting");
    Assert.assertEquals(1, processorsFactory.getPreProcessors().size());
  }
  
  @Test
  public void testWithEmptyPostProcessors() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS)).thenReturn("");
    Assert.assertTrue(processorsFactory.getPostProcessors().isEmpty());
  }
  
  @Test(expected = WroRuntimeException.class)
  public void cannotUseInvalidPostProcessorsSet() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS)).thenReturn(
        "INVALID1,INVALID2");
    processorsFactory.getPostProcessors();
  }
  
  @Test
  public void testWhenValidPostProcessorsSet() {
    createManager();
    
    Mockito.when(mockFilterConfig.getInitParameter(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS)).thenReturn(
        "cssMinJawr, jsMin, cssVariables");
    Assert.assertEquals(3, processorsFactory.getPostProcessors().size());
  }
  
  @Test
  public void testConfigPropertiesWithValidPreProcessor() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, "cssMin");
    victim.setConfigProperties(configProperties);
    
    createManager();
    
    Collection<ResourceProcessor> list = processorsFactory.getPreProcessors();
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(CssMinProcessor.class, list.iterator().next().getClass());
  }
  
  @Test
  public void testConfigPropertiesWithValidPostProcessor() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, "jsMin");
    victim.setConfigProperties(configProperties);
    
    createManager();
    
    Assert.assertEquals(1, processorsFactory.getPostProcessors().size());
    Assert.assertEquals(JSMinProcessor.class, processorsFactory.getPostProcessors().iterator().next().getClass());
  }
  
  @Test
  public void testConfigPropertiesWithMultipleValidPostProcessor() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, "jsMin, cssMin");
    victim.setConfigProperties(configProperties);
    
    createManager();
    
    Assert.assertEquals(2, processorsFactory.getPostProcessors().size());
    Assert.assertEquals(JSMinProcessor.class, processorsFactory.getPostProcessors().iterator().next().getClass());
  }
  
  @Test(expected = WroRuntimeException.class)
  public void testConfigPropertiesWithInvalidPreProcessor() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, "INVALID");
    victim.setConfigProperties(configProperties);
    
    createManager();
    
    processorsFactory.getPreProcessors();
  }
  
  public void shouldUseExtensionAwareProcessorWhenProcessorNameContainsDotCharacter() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, "jsMin.js");
    victim.setConfigProperties(configProperties);
    Assert.assertEquals(1, processorsFactory.getPreProcessors().size());
    Assert.assertTrue(processorsFactory.getPreProcessors().iterator().next() instanceof ExtensionsAwareProcessorDecorator);
  }
  
  @Test(expected = WroRuntimeException.class)
  public void testConfigPropertiesWithInvalidPostProcessor() {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, "INVALID");
    victim.setConfigProperties(configProperties);
    
    createManager();
    
    processorsFactory.getPostProcessors();
  }
  
  @Test(expected = WroRuntimeException.class)
  public void cannotConfigureInvalidNamingStrategy() throws Exception {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableNamingStrategy.KEY, "INVALID");
    victim.setConfigProperties(configProperties);
    victim.create().getNamingStrategy().rename("name", WroUtil.EMPTY_STREAM);
  }
  
  @Test
  public void shouldUseConfiguredNamingStrategy() throws Exception {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableNamingStrategy.KEY, TimestampNamingStrategy.ALIAS);
    victim.setConfigProperties(configProperties);
    final NamingStrategy actual = ((ConfigurableNamingStrategy) victim.create().getNamingStrategy()).getConfiguredStrategy();
    Assert.assertEquals(TimestampNamingStrategy.class, actual.getClass());
  }
  
  @Test(expected = WroRuntimeException.class)
  public void cannotConfigureInvalidHashStrategy() throws Exception {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableHashStrategy.KEY, "INVALID");
    victim.setConfigProperties(configProperties);
    victim.create().getHashStrategy().getHash(WroUtil.EMPTY_STREAM);
  }
  
  @Test
  public void shouldUseConfiguredHashStrategy() throws Exception {
    final Properties configProperties = new Properties();
    configProperties.setProperty(ConfigurableHashStrategy.KEY, MD5HashStrategy.ALIAS);
    victim.setConfigProperties(configProperties);
    final HashStrategy actual = ((ConfigurableHashStrategy) victim.create().getHashStrategy()).getConfiguredStrategy();
    Assert.assertEquals(MD5HashStrategy.class, actual.getClass());
  }
}
