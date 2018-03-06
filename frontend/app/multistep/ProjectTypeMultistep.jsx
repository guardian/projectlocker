import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import ProjectTypeComponent from './projecttype/ProjectTypeComponent.jsx';
import ProjectTypeCompletionComponent from './projecttype/CompletionComponent.jsx';

class ProjectTypeMultistep extends React.Component {
    static propTypes = {
        match: PropTypes.object.required
    };

    constructor(props){
        super(props);

        this.state = {
            projectType: null,
            currentEntry: null,
            postrunList: [],
            loading: false,
            loadingError: null
        }
    }

    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid})
        }
        this.loadPostrunActions();
    }

    loadPostrunActions(){
        //load in the list of postrun actions from the server
        this.setState({loading: true},()=> {
            axios.get("/api/postrun")
                .then(response => this.setState({postrunList: response.data.result, loading: false}))
                .catch(error => this.setState({loadingError: error, loading:false}))
        });
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
