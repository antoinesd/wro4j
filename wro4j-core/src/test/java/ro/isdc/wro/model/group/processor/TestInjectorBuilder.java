/**
 * Copyright Alex Objelean
 */
package ro.isdc.wro.model.group.processor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ro.isdc.wro.config.Context.set;

import java.io.IOException;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ro.isdc.wro.cache.CacheStrategy;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.config.metadata.MetaDataFactory;
import ro.isdc.wro.manager.callback.LifecycleCallbackRegistry;
import ro.isdc.wro.manager.factory.BaseWroManagerFactory;
import ro.isdc.wro.manager.factory.WroManagerFactory;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.GroupExtractor;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.locator.factory.DefaultResourceLocatorFactory;
import ro.isdc.wro.model.resource.locator.factory.ResourceLocatorFactory;
import ro.isdc.wro.model.resource.processor.factory.ProcessorsFactory;
import ro.isdc.wro.model.resource.support.hash.HashStrategy;
import ro.isdc.wro.model.resource.support.naming.NamingStrategy;
import ro.isdc.wro.util.AbstractDecorator;
import ro.isdc.wro.util.WroUtil;


/**
 * @author Alex Objelean
 * @created 8 Jan 2012
 */
public class TestInjectorBuilder {
  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  @Mock
  private FilterConfig mockFilterConfig;
  @Mock
  private ServletContext mockServletContext;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(""));
    when(mockRequest.getServletPath()).thenReturn("");
    when(mockFilterConfig.getServletContext()).thenReturn(mockServletContext);
    when(mockFilterConfig.getServletContext()).thenReturn(mockServletContext);
    when(mockServletContext.getResourceAsStream(Mockito.anyString())).thenReturn(null);
    set(Context.webContext(mockRequest, mockResponse, mockFilterConfig));
  }

  @Test(expected = NullPointerException.class)
  public void cannotAcceptNullWroManager() {
    new InjectorBuilder(null);
  }

  @Test(expected = NullPointerException.class)
  public void cannotAcceptWhenSettingNullWroManager() {
    new InjectorBuilder(null);
  }

  @Test
  public void shouldBuildInjectorWithValidWroManager() {
    final Injector injector = InjectorBuilder.create(new BaseWroManagerFactory()).build();
    assertNotNull(injector);

    final Sample sample = new Sample();
    injector.inject(sample);
    assertNotNull(sample.namingStrategy);
    assertNotNull(sample.preProcessorExecutor);
    assertNotNull(sample.processorsFactory);
    assertNotNull(sample.resourceLocatorFactory);
    assertNotNull(sample.callbackRegistry);
    assertSame(injector, sample.injector);
    assertNotNull(sample.groupsProcessor);
    assertNotNull(sample.metaDataFactory);
  }

  @Test
  public void shouldBuildValidInjectorWithBaseWroManagerFactory() {
    final Injector injector = InjectorBuilder.create(new BaseWroManagerFactory()).build();
    Assert.assertNotNull(injector);

    final Sample sample = new Sample();
    injector.inject(sample);
    assertNotNull(sample.namingStrategy);
    assertNotNull(sample.preProcessorExecutor);
    assertNotNull(sample.processorsFactory);
    assertNotNull(sample.resourceLocatorFactory);
    assertNotNull(sample.callbackRegistry);
    assertSame(injector, sample.injector);
    assertNotNull(sample.groupsProcessor);
    assertNotNull(sample.metaDataFactory);
  }

  @Test
  public void shouldBuildValidInjectorWithFewFieldsSet() throws Exception {
    final NamingStrategy mockNamingStrategy = Mockito.mock(NamingStrategy.class);
    final ProcessorsFactory mockProcessorsFactory = Mockito.mock(ProcessorsFactory.class);
    final ResourceLocatorFactory mockLocatorFactory = Mockito.mock(ResourceLocatorFactory.class);
    final MetaDataFactory mockMetaDataFactory = Mockito.mock(MetaDataFactory.class);

    final BaseWroManagerFactory managerFactroy = new BaseWroManagerFactory();
    managerFactroy.setNamingStrategy(mockNamingStrategy);
    managerFactroy.setProcessorsFactory(mockProcessorsFactory);
    managerFactroy.setLocatorFactory(mockLocatorFactory);
    managerFactroy.setMetaDataFactory(mockMetaDataFactory);

    final Injector injector = InjectorBuilder.create(managerFactroy).build();
    assertNotNull(injector);

    final Sample sample = new Sample();
    injector.inject(sample);
    assertNotNull(sample.preProcessorExecutor);

    sample.namingStrategy.rename("", WroUtil.EMPTY_STREAM);
    verify(mockNamingStrategy).rename("", WroUtil.EMPTY_STREAM);

    sample.processorsFactory.getPostProcessors();
    verify(mockProcessorsFactory).getPostProcessors();

    sample.resourceLocatorFactory.getLocator("");
    verify(mockLocatorFactory).getLocator("");

    sample.metaDataFactory.create();
    verify(mockMetaDataFactory).create();

    assertSame(mockProcessorsFactory, AbstractDecorator.getOriginalDecoratedObject(sample.processorsFactory));
    assertSame(mockLocatorFactory, AbstractDecorator.getOriginalDecoratedObject(sample.resourceLocatorFactory));

    assertNotNull(sample.callbackRegistry);
    assertSame(injector, sample.injector);
    assertNotNull(sample.groupsProcessor);
    assertNotNull(sample.modelFactory);
    assertNotNull(sample.groupExtractor);
    assertNotNull(sample.cacheStrategy);
    assertNotNull(sample.hashBuilder);
    assertNotNull(sample.readOnlyContext);
    org.junit.Assert.assertNotNull(sample.metaDataFactory);
  }

  @Test(expected = IOException.class)
  public void shouldInjectEachLocatorProvidedByLocatorFactory()
      throws Exception {
    final ResourceLocatorFactory resourceLocatorFactory = new DefaultResourceLocatorFactory();
    final WroManagerFactory managerFactory = new BaseWroManagerFactory().setLocatorFactory(resourceLocatorFactory);
    final Injector injector = InjectorBuilder.create(managerFactory).build();

    final Sample sample = new Sample();
    injector.inject(sample);
    // this will throw NullPointerException if the uriLocator is not injected.
    sample.resourceLocatorFactory.getLocator("/path/to/servletContext/resource.js").getInputStream();
  }

  @After
  public void tearDown() {
    Context.unset();
  }

  private static class Sample {
    @Inject
    ResourceLocatorFactory resourceLocatorFactory;
    @Inject
    ProcessorsFactory processorsFactory;
    @Inject
    NamingStrategy namingStrategy;
    @Inject
    PreProcessorExecutor preProcessorExecutor;
    @Inject
    LifecycleCallbackRegistry callbackRegistry;
    @Inject
    Injector injector;
    @Inject
    GroupsProcessor groupsProcessor;
    @Inject
    WroModelFactory modelFactory;
    @Inject
    GroupExtractor groupExtractor;
    @Inject
    CacheStrategy<?, ?> cacheStrategy;
    @Inject
    HashStrategy hashBuilder;
    @Inject
    ReadOnlyContext readOnlyContext;
    @Inject
    MetaDataFactory metaDataFactory;
  }
}
