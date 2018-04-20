import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import TypeSelectorComponent from './projecttemplate/TypeSelectorComponent.jsx';
import TemplateUploadComponent from './projecttemplate/TemplateUploadComponent.jsx';
import TemplateCompletionComponent from './projecttemplate/CompletionComponent.jsx';
import CommonMultistepComponent from "./common/CommonMultistepComponent.jsx";

class ProjectTemplateMultistep extends CommonMultistepComponent
{
    constructor(props) {
        super(props);
        this.state = {
            template: null,
            projectTypes: [],
            plutoProjectTypesList: [],
            selectedType: null,
            selectedPlutoSubtype: "",
            currentEntry: null,
            error: null,
            fileId: null,
            name: "",
            storages: [],
            loadingComplete: false
        }
    }

    componentWillMount() {
        if (this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid !== "new") {
            this.setState({currentEntry: this.props.match.params.itemid}, ()=>this.loadTemplateData())
        } else {
            this.loadTemplateData();
        }
    }

    loadTemplateData(){
        let promiseList = [axios.get("/api/projecttype"), axios.get("/api/storage"), axios.get("/api/plutoprojecttypeid")];
        if(this.state.currentEntry) promiseList.push(axios.get("/api/template/" + this.state.currentEntry));

        Promise.all(promiseList)
            .then(responses=>{
                const projectTypeResponse = responses[0];
                const storageResponse = responses[1];
                const plutoTypeResponse = responses[2];

                const firstType = projectTypeResponse.data.result[0] ? projectTypeResponse.data.result[0].id : null;

                const baseData = {
                    projectTypes: projectTypeResponse.data.result,
                    selectedType: firstType,
                    storages: storageResponse.data.result,
                    plutoProjectTypesList: plutoTypeResponse.data.result,
                    loadingComplete: true
                };

                if(this.state.currentEntry){
                    const loadedEntry = responses[3].data;
                    const currentEntryData = {
                        selectedType: loadedEntry.result.projectTypeId,
                        fileId: loadedEntry.result.fileRef,
                        selectedFileId: loadedEntry.result.fileRef,
                        name: loadedEntry.result.name,
                        selectedPlutoSubtype: loadedEntry.result.hasOwnProperty('plutoSubtype') ? loadedEntry.result.plutoSubtype : "",
                        loadingComplete: true
                    };
                    this.setState(Object.assign({}, baseData, currentEntryData));
                } else {
                    this.setState(baseData);
                }

            }).catch(error=>{
                console.error(error);
                this.setState({error: error});
            });
    }

    render() {
        const steps = [
            {
                name: 'Project Type',
                component: <TypeSelectorComponent projectTypes={this.state.projectTypes}
                                                  selectedType={this.state.selectedType}
                                                  templateName={this.state.name}
                                                  plutoTypesList={this.state.plutoProjectTypesList}
                                                  selectedPlutoSubtype={this.state.selectedPlutoSubtype}
                                                  loadingComplete={this.state.loadingComplete}
                                                  valueWasSet={(nameAndType)=>this.setState({selectedType: nameAndType.selectedType, name: nameAndType.name, selectedPlutoSubtype: nameAndType.selectedPlutoSubtype})}/>
            },
            {
                name: 'Upload template',
                component: <TemplateUploadComponent storages={this.state.storages}
                                                    existingFileId={this.state.fileId}
                                                    valueWasSet={(fileId)=>this.setState({selectedFileId: fileId})}/>
            },
            {
                name: 'Confirm',
                component: <TemplateCompletionComponent currentEntry={this.state.currentEntry}
                                                        fileId={this.state.selectedFileId}
                                                        name={this.state.name}
                                                        projectType={this.state.selectedType}
                                                        plutoSubtype={this.state.selectedPlutoSubtype}
                />
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTemplateMultistep;