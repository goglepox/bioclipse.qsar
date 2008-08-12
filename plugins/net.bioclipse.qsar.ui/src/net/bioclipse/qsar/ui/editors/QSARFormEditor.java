/*******************************************************************************
 *Copyright (c) 2008 The Bioclipse Team and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *Contributors:
 *    Ola Spjuth - initial API and implementation
 *******************************************************************************/
package net.bioclipse.qsar.ui.editors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.log4j.Logger;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.qsar.DocumentRoot;
import net.bioclipse.qsar.QsarFactory;
import net.bioclipse.qsar.QsarPackage;
import net.bioclipse.qsar.QsarType;
import net.bioclipse.qsar.util.QsarAdapterFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;

public class QSARFormEditor extends FormEditor implements IResourceChangeListener, IAdaptable{

    private static final Logger logger = Logger.getLogger(QSARFormEditor.class);
    
    private TextEditor textEditor;
    private MoleculesPage molPage;
    
    private IProject activeProject;

	private QsarType qsarModel;

	private Resource resource;

	private ResourceSetImpl resourceSet;

	private int textEditorIndex;

	private int molPageIndex;

	private AdapterFactoryEditingDomain editingDomain;
    
    
    public IProject getActiveProject() {
    
        return activeProject;
    }

    
    public void setActiveProject( IProject activeProject ) {
    
        this.activeProject = activeProject;
    }

    /**
     * Overrides super to plug in a different selection provider.
     */
    public void init(IEditorSite site, IEditorInput input)
    throws PartInitException {
        super.init(site, input);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        initialize();
    }

    /**
     * Do customizations based on project
     */
    private void initialize() {

		QsarAdapterFactory factory=new QsarAdapterFactory();
		editingDomain=new AdapterFactoryEditingDomain(factory, new BasicCommandStack());

    	
    	//Get project
        if (!(getEditorInput() instanceof IFileEditorInput)) {
            return;
        }

        
        try {
			parseInput();
		} catch (CoreException e) {
            logger.error("Could not parse input: " + e.getMessage());
			e.printStackTrace();
			
			//Close editor?
		}

        //Get molecules folder if exists
        IFolder molFolder=activeProject.getFolder("molecules");
        if (!(molFolder.exists())){
            logger.error("Folder 'molecules'  does not exist.");
        }

    }

    //Read and parse with EMF
    private void parseInput() throws CoreException {

    	IFileEditorInput input = (IFileEditorInput) getEditorInput();
        activeProject=input.getFile().getProject();
        
        IFile file=input.getFile();


        // Create a resource set.
        resourceSet = new ResourceSetImpl();

        // Register the default resource factory -- only needed for stand-alone!
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
        		Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        // Register the package -- only needed for stand-alone!
        //Is this needed?
        QsarPackage qsarPackage=QsarPackage.eINSTANCE;

        // Get the URI of the model file.
        URI fileURI = URI.createPlatformResourceURI(file.getFullPath().toString(), false);

        // Demand load the resource for this file.
        resource = resourceSet.getResource(fileURI, true);
        
        if (resource.getContents()==null){
        	throw new PartInitException("No contents in parsed resource");
        }

        //Should be only one docroot
        if (!(resource.getContents().get(0) instanceof DocumentRoot)) {
        	throw new PartInitException("Documentroot is not of QSAR type");
		}
		DocumentRoot root = (DocumentRoot) resource.getContents().get(0);

		//Store the model
		qsarModel=root.getQsar();

        // Print the contents of the resource to System.out.
        try
        {
        	resource.save(System.out, Collections.EMPTY_MAP);
        }
        catch (IOException e) {
        }

	}



	@Override
    protected void addPages() {

		//Create the MoleculesPage
		molPage=new MoleculesPage(this, qsarModel, editingDomain);

        try {
            //Molecules page with interactions
            molPageIndex=addPage(molPage);
            setPageText(molPageIndex, "Molecules");

            //Descriptors page
//            addPage(descPage);

            //Texteditor, should be XMLEditor: TODO
            textEditor = new TextEditor();
            textEditorIndex = addPage(textEditor, getEditorInput());
            setPageText(textEditorIndex, "Source");

        } catch (PartInitException e) {
            LogUtils.debugTrace(logger, e);
        }
    }



    @Override
    public void doSave(IProgressMonitor monitor) {
    	
    	//Take QSAR model and save it
		try {

			//Clear resource and add current model
			resource.getContents().clear();
			DocumentRoot root=QsarFactory.eINSTANCE.createDocumentRoot();
			root.setQsar(qsarModel);
			
			resource.getContents().add(root);
			resource.save(Collections.EMPTY_MAP);

			//Serialize to byte[] and print to sysout
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			resource.save(os, Collections.EMPTY_MAP);

			System.out.println(new String(os.toByteArray()));

			System.out.println("end");
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }

	@Override
	public void doSaveAs() {
	}

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
    
    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    public void resourceChanged(IResourceChangeEvent event) {
        //TODO
    }
    
    @Override
    protected void pageChange(int newPageIndex) {
    	
    	if (newPageIndex==textEditorIndex){
    		//Serialize to XML
            // Print the contents of the resource to System.out.
            try
            {
            	ByteArrayOutputStream bos=new ByteArrayOutputStream();
            	resource.save(bos, Collections.EMPTY_MAP);
            	
            	String strContent=new String(bos.toByteArray());
            	
                textEditor.getDocumentProvider().
                getDocument(textEditor.getEditorInput()).set(strContent);
            }
            catch (IOException e) {
            }
    		
    	}
    	
    	// TODO Auto-generated method stub
    	super.pageChange(newPageIndex);
    }



}
