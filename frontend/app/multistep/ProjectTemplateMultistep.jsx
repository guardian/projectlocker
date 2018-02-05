import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import TypeSelectorComponent from './projecttemplate/TypeSelectorComponent.jsx';
import TemplateCompletionComponent from './projecttemplate/CompletionComponent.jsx';
import CommonMultistepComponent from "./common/CommonMultistepComponent.jsx";

class ProjectTemplateMultistep extends CommonMultistepComponent
{
    constructor(props) {
        super(props);
        this.state = {
            template: null,
            projectTypes: null,
            selectedType: null,
            currentEntry: null
        }
    }

    componentWillMount() {
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid})
        }
        axios.get("/api/projecttype").then((response)=>{
            this.setState({projectTypes: response.data});
        }).catch((error)=>{
            console.error(error);
        })
    }

    render() {
        const steps = [
            {
                name: 'Project Type',
                component: <TypeSelectorComponent projectTypes={this.state.projectTypes} valueWasSet={(type)=>this.setState({selectedType: type})}/>
            },
            {
                name: 'Confirm',
                component: <TemplateCompletionComponent currentEntry={this.state.currentEntry} template={this.state.template}/>
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTemplateMultistep;