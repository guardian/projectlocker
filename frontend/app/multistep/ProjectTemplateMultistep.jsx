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
            storages: []
        }
    }

    componentWillMount() {
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid})
        }

        Promise.all([axios.get("/api/projecttype"), axios.get("/api/storage"), axios.get("/api/plutoprojecttypeid")])
            .then(responses=>{
                const projectTypeResposne = responses[0];
                const storageResponse = responses[1];
                const plutoTypeResponse = responses[2];

            const firstType = projectTypeResposne.data.result[0] ? projectTypeResposne.data.result[0].id : null;

            this.setState({projectTypes: projectTypeResposne.data.result,
                selectedType: firstType,
                storages: storageResponse.data.result,
                plutoProjectTypesList: plutoTypeResponse.data.result
            });

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
                                                  valueWasSet={(nameAndType)=>this.setState({selectedType: nameAndType.selectedType, name: nameAndType.name, selectedPlutoSubtype: nameAndType.selectedPlutoSubtype})}/>
            },
            {
                name: 'Upload template',
                component: <TemplateUploadComponent storages={this.state.storages} valueWasSet={(fileId)=>this.setState({selectedFileId: fileId})}/>
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