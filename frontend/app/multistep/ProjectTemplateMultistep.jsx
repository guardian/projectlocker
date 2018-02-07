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
            selectedType: null,
            currentEntry: null,
            error: null,
            fileId: null,
            storages: []
        }
    }

    componentWillMount() {
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid})
        }
        axios.get("/api/projecttype").then((response)=>{
            this.setState({projectTypes: response.data.result});
        }).catch((error)=>{
            console.error(error);
            this.setState({error: error})
        });

        /*update storage type list*/
        axios.get("/api/storage").then(response=>{
            this.setState({storages: response.data.result});
        }).catch(error=>{
            this.setState({error: error});
        });
    }

    render() {
        const steps = [
            {
                name: 'Project Type',
                component: <TypeSelectorComponent projectTypes={this.state.projectTypes} valueWasSet={(nameAndType)=>this.setState({selectedType: nameAndType.selectedType, name: nameAndType.name})}/>
            },
            {
                name: 'Upload template',
                component: <TemplateUploadComponent storages={this.state.storages} valueWasSet={(fileId)=>this.setState({selectedFileId: fileId})}/>
            },
            {
                name: 'Confirm',
                component: <TemplateCompletionComponent currentEntry={this.state.currentEntry} fileId={this.state.selectedFileId} name={this.state.name} projectType={this.state.selectedType}/>
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTemplateMultistep;