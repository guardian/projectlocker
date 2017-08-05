import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import TypeSelectorComponent from './projecttemplate/TypeSelectorComponent.jsx';

class ProjectTemplateMultistep extends React.Component
{
    constructor(props) {
        super(props);
        this.state = {
            projectTypes: null,
            selectedType: null
        }
    }

    componentWillMount() {
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
                name: 'Upload'
            },
            {
                name: 'Confirm'
            }
        ]
    }
}

export default ProjectTemplateMultistep;