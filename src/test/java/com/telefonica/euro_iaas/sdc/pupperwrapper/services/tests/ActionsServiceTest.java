/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U <br>
 * This file is part of FI-WARE project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * </p>
 * <p>
 * You may obtain a copy of the License at:<br>
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * </p>
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * </p>
 * <p>
 * See the License for the specific language governing permissions and limitations under the License.
 * </p>
 * <p>
 * For those usages not covered by the Apache version 2.0 License please contact with opensource@tid.es
 * </p>
 */

package com.telefonica.euro_iaas.sdc.pupperwrapper.services.tests;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.jgit.lib.AnyObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.telefonica.euro_iaas.sdc.puppetwrapper.common.Action;
import com.telefonica.euro_iaas.sdc.puppetwrapper.data.Node;
import com.telefonica.euro_iaas.sdc.puppetwrapper.data.Software;
import com.telefonica.euro_iaas.sdc.puppetwrapper.services.CatalogManager;
import com.telefonica.euro_iaas.sdc.puppetwrapper.services.FileAccessService;
import com.telefonica.euro_iaas.sdc.puppetwrapper.services.impl.CatalogManagerMongoImpl;
import com.telefonica.euro_iaas.sdc.puppetwrapper.services.impl.FileAccessServiceImpl;
import com.telefonica.euro_iaas.sdc.puppetwrapper.services.impl.ProcessBuilderFactory;

public class ActionsServiceTest {

    private ActionServiceImpl4Test actionsService;

    private CatalogManager catalogManagerMongo;

    private ProcessBuilderFactory processBuilderFactory;
    
    private Node node1;
    private Node node1Modified;

    @Before
    public void setUpMock() throws Exception {
        catalogManagerMongo = mock(CatalogManagerMongoImpl.class);

        FileAccessService fileAccessService = mock(FileAccessServiceImpl.class);

        processBuilderFactory = mock(ProcessBuilderFactory.class);

        actionsService = new ActionServiceImpl4Test();
        actionsService.setCatalogManager(catalogManagerMongo);
        actionsService.setFileAccessService(fileAccessService);
        actionsService.setProcessBuilderFactory(processBuilderFactory);

        node1 = new Node();
        node1.setGroupName("testGroup");
        node1.setId("1");
        Software soft1 = new Software();
        soft1.setName("testSoft");
        soft1.setAction(Action.INSTALL);
        soft1.setVersion("1.0.0");
        node1.addSoftware(soft1);
        
        node1Modified = new Node();
        node1Modified.setGroupName("testGroup");
        node1Modified.setId("1");
        Software soft1Modified = new Software();
        soft1Modified.setName("testSoft");
        soft1Modified.setAction(Action.INSTALL);
        soft1Modified.setVersion("2.0.0");
        node1.addSoftware(soft1Modified);

    }

    @Test
    public void install() {
        
        when(catalogManagerMongo.getNode("1")).thenThrow(new NoSuchElementException()).thenReturn(node1);

        actionsService.action(Action.INSTALL, "testGroup", "1", "testSoft", "1.0.0");

        Node node = catalogManagerMongo.getNode("1");
        Software soft = node.getSoftware("testSoft");

        assertTrue(node != null);
        assertTrue(soft != null);
        assertTrue(node.getGroupName().equals("testGroup"));
        assertTrue(node.getId().equals("1"));
        assertTrue(soft.getName().equals("testSoft"));
        assertTrue(soft.getVersion().equals("1.0.0"));
        assertTrue(soft.getAction().equals(Action.INSTALL));

    }

    @Test
    public void uninstallTest() {
        
        when(catalogManagerMongo.getNode("1")).thenReturn(node1);

        Node node = actionsService.action(Action.UNINSTALL, "testGroup", "1", "testSoft", "1.0.0");

        Software soft = node.getSoftware("testSoft");

        assertTrue(node != null);
        assertTrue(soft != null);
        assertTrue(node.getGroupName().equals("testGroup"));
        assertTrue(node.getId().equals("1"));
        assertTrue(soft.getName().equals("testSoft"));
        assertTrue(soft.getVersion().equals("1.0.0"));
        assertTrue(soft.getAction().equals(Action.UNINSTALL));

    }

    @Test
    public void uninstall_Modification_Soft() {
        
        when(catalogManagerMongo.getNode("1")).thenReturn(node1);

        actionsService.action(Action.UNINSTALL, "testGroup", "1", "testSoft", "1.0.0");
        actionsService.action(Action.UNINSTALL, "testGroup", "1", "testSoft", "2.0.0");

        Node node = catalogManagerMongo.getNode("1");
        Software soft = node.getSoftware("testSoft");

        verify(catalogManagerMongo, times(2)).addNode((Node) anyObject());

    }
    
    @Test(expected = NoSuchElementException.class)
    public void uninstall_soft_not_exists() {
        
        when(catalogManagerMongo.getNode("1")).thenReturn(node1);

        actionsService.action(Action.UNINSTALL, "testGroup", "1", "testSoftNoExists", "1.0.0");
        
        verify(catalogManagerMongo, times(1)).getNode(anyString());
    }

    @Test(expected = NoSuchElementException.class)
    public void uninstall_node_not_esxists() {

        when(catalogManagerMongo.getNode("nodenoexists")).thenThrow(new NoSuchElementException());

        actionsService.action(Action.UNINSTALL, "groupnoexists", "nodenoexists", "testSoft", "1.0.0");
        
        verify(catalogManagerMongo, times(1)).getNode(anyString());
    }

    @Test(expected = NoSuchElementException.class)
    public void uninstall_soft_not_esxists() {

        when(catalogManagerMongo.getNode("nodenoexists")).thenThrow(new NoSuchElementException());

        actionsService.action(Action.UNINSTALL, "testGroup", "nodenoexists", "softnoexists", "1.0.0");
    }

    @Test
    public void deleteNodeTest_OK() throws IOException {

        Process shell = mock(Process.class);
        Process shell2 = mock(Process.class);

        String[] cmd = { anyString() };
        // call to puppet cert list --all
        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell).thenReturn(shell2);

        String str = "Node 1 is registered";
        String strdelete = "Node 1 unregistered";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8"))).thenReturn(
                new ByteArrayInputStream(strdelete.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        String str2 = "Node1.novalocal";
        when(shell2.getInputStream()).thenReturn(new ByteArrayInputStream(str2.getBytes("UTF-8")));

        String strEr2 = " ";
        when(shell2.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr2.getBytes("UTF-8")));

        when(catalogManagerMongo.getNode("1")).thenThrow(new NoSuchElementException()).thenReturn(node1);
        
        actionsService.deleteNode("1");
        
        verify(shell,times(1)).getInputStream();
        verify(shell2,times(2)).getInputStream();
        verify(processBuilderFactory,times(3)).createProcessBuilder((String[])anyObject());


    }

    @Test(expected = IOException.class)
    public void deleteNodeTest_Exception() throws IOException {

        Process shell = mock(Process.class);

        String[] cmd = { anyString() };
        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell);

        String str = "";
        String strdelete = "";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8"))).thenReturn(
                new ByteArrayInputStream(strdelete.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        when(catalogManagerMongo.getNode("1")).thenThrow(new NoSuchElementException()).thenReturn(node1);

        // delete node 1

        actionsService.deleteNode("1");
        
        verify(shell,times(1)).getInputStream();
        verify(processBuilderFactory,times(3)).createProcessBuilder((String[])anyObject());


    }

    @Test
    public void isNodeRegistered_NO() throws IOException {

        Process shell = mock(Process.class);

        String[] cmd = { anyString() };
        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell);

        String str = "Node 3 is registered";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        Assert.assertFalse(actionsService.isNodeRegistered("1"));

    }

    @Test
    public void isNodeRegistered_YES() throws IOException {

        Process shell = mock(Process.class);

        String[] cmd = { anyString() };

        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell);

        String str = "Node 1 is registered";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        Assert.assertTrue(actionsService.isNodeRegistered("1"));

    }

    @Test(expected = IOException.class)
    public void isNodeRegistered_Exception() throws IOException {

        Process shell = mock(Process.class);

        String[] cmd = { anyString() };
        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell);

        String str = "";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        Assert.assertTrue(actionsService.isNodeRegistered("1"));

    }

    @Test()
    public void getRealNodeNameTest() throws IOException {

        Process shell = mock(Process.class);

        String[] cmd = { anyString() };

        when(processBuilderFactory.createProcessBuilder(cmd)).thenReturn(shell);

        String str = "\"testnodename.openstacklocal\"";
        when(shell.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes("UTF-8")));

        String strEr = " ";
        when(shell.getErrorStream()).thenReturn(new ByteArrayInputStream(strEr.getBytes("UTF-8")));

        Assert.assertTrue("testnodename.openstacklocal".equals(actionsService.getRealNodeName("testnodename")));

    }

}
