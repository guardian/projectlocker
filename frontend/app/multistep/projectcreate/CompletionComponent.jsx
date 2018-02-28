import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class ProjectCompletionComponent extends React.Component {
    static propTypes = {
        projectTemplates: PropTypes.array.isRequired,
        selectedProjectTemplate: PropTypes.number.isRequired,
        storages: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        projectName: PropTypes.string.isRequired,
        projectFilename: PropTypes.string.isRequired,
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    requestContent(){
        return {
            filename: this.props.projectFilename,
            destinationStorageId: this.props.selectedStorage,
            title: this.props.projectName,
            projectTemplateId: this.props.selectedProjectTemplate,
            user: "frontend"    //this should be deprecated as the backend ignores it
        };
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        axios.request({method: "PUT", url: "/api/project",data: this.requestContent()}).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location.assign('/project/');
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }

    render() {
        const errorLabel = this.state.error ? <span className="error-text">{this.state.error.statusText}: {this.state.error.data.detail}</span> : "";

        return(<div>
            <h3>Create new project</h3>
            <p className="information">We will create a new project with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent projectTemplates={this.props.projectTemplates} selectedProjectTemplate={this.props.selectedProjectTemplate}
                              storages={this.props.storages} selectedStorage={this.props.selectedStorage}
                              projectName={this.props.projectName} projectFilename={this.props.projectFilename}/>
            
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}>{errorLabel}<button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectCompletionComponent;
