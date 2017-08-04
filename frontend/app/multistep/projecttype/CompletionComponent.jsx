import React from 'react';
import axios from 'axios';
import SummaryComponent from './SummaryComponent.jsx';

class ProjectTypeCompletionComponent extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        axios.put("/api/projecttype",this.requestContent()).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location.assign('/projecttype/');
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }

    requestContent(){
        /* returns an object of keys/values to send to the server for saving */
        return {
            name: this.props.projectType.name,
            opensWith: this.props.projectType.opensWith,
            targetVersion: this.props.projectType.version
        }
    }

    render() {
        return(<div>
            <h3>Set up storage</h3>
            <p className="information">We will set up a new project type definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent name={this.props.projectType.name} opensWith={this.props.projectType.opensWith} version={this.props.projectType.version}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectTypeCompletionComponent;
