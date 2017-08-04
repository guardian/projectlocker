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
        }
    }

    render(){
        const steps = [
            {
                name: 'Project type',
                component: <ProjectTypeComponent
                    currentProjectType={this.props.currentEntry}
                    valueWasSet={(newtype)=>this.setState({projectType: newtype})}
                />
            },
            {
                name: 'Confirm',
                component: <ProjectTypeCompletionComponent projectType={this.state.projectType}/>
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTypeMultistep;
