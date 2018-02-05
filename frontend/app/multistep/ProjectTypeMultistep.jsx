import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';

import ProjectTypeComponent from './projecttype/ProjectTypeComponent.jsx';
import ProjectTypeCompletionComponent from './projecttype/CompletionComponent.jsx';

class ProjectTypeMultistep extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            projectType: null,
            currentEntry: null
        }
    }

    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid){
            this.setState({currentEntry: this.props.match.params.itemid})
        }
    }

    render(){
        const steps = [
            {
                name: 'Project type',
                component: <ProjectTypeComponent
                    currentEntry={this.state.currentEntry}
                    valueWasSet={(newtype)=>{ console.log("valueWasSet"); this.setState({projectType: newtype})}}
                />
            },
            {
                name: 'Confirm',
                component: <ProjectTypeCompletionComponent projectType={this.state.projectType} currentEntry={this.state.currentEntry}/>
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTypeMultistep;
