import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class ProjectTypeCompletionComponent extends React.Component {
    static propTypes = {
        currentEntry: PropTypes.object.isRequired,
        postrunActions: PropTypes.array.isRequired,
        selectedPostruns: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    savePostruns(projecttypeid) {
        const promiseList = this.props.selectedPostruns.map(postrunId => axios.put("/api/postrun/" + postrunId + "/projecttype/" + projecttypeid))
        return Promise.all(promiseList)
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        const restUrl = this.props.currentEntry ? "/api/projecttype/" + this.props.currentEntry : "/api/projecttype";

        axios.put(restUrl,this.requestContent()).then(
            (response)=>{
                this.savePostruns(response.data.id)
                    .then(()=>{
                        this.setState({inProgress: false});
                        window.location.assign('/type/');
                    });
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
            targetVersion: this.props.projectType.version,
            fileExtension: this.props.projectType.fileExtension
        }
    }

    render() {
        return(<div>
            <h3>Set up project type</h3>
            <p className="information">We will set up a new project type definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent name={this.props.projectType.name}
                              opensWith={this.props.projectType.opensWith}
                              version={this.props.projectType.version}
                              fileExtension={this.props.projectType.fileExtension}
                              postrunActions={this.props.postrunActions}
                              selectedPostruns={this.props.selectedPostruns}
            />
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectTypeCompletionComponent;
