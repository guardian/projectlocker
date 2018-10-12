import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import LongProcessComponent from './LongProcessComponent.jsx';

class ProjectCompletionComponent extends React.Component {
    static propTypes = {
        projectTemplates: PropTypes.array.isRequired,
        selectedProjectTemplate: PropTypes.number.isRequired,
        storages: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        projectName: PropTypes.string.isRequired,
        projectFilename: PropTypes.string.isRequired,
        selectedWorkingGroupId: PropTypes.number.isRequired,
        selectedCommissionId: PropTypes.number.isRequired,
        wgList: PropTypes.array.isRequired,
        deletable: PropTypes.bool.isRequired,
        deep_archive: PropTypes.bool.isRequired,
        sensitive: PropTypes.bool.isRequired
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
            destinationStorageId: parseInt(this.props.selectedStorage),
            title: this.props.projectName,
            projectTemplateId: parseInt(this.props.selectedProjectTemplate),
            user: "frontend",    //this should be deprecated as the backend ignores it
            workingGroupId: this.props.selectedWorkingGroupId ? parseInt(this.props.selectedWorkingGroupId) : null,
            commissionId: this.props.selectedCommissionId ? parseInt(this.props.selectedCommissionId ) : null,
            deletable: this.props.deletable,
            deepArchive: this.props.deep_archive,
            sensitive: this.props.sensitive
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

    getWarnings(){
        let list=[];
        if(this.props.selectedWorkingGroupId===null || this.props.selectedWorkingGroupId===0)
            list.push("If you don't select a working group, asset folder creation will fail");
        if(this.props.selectedCommissionId===null || this.props.selectedCommissionId===0)
            list.push("If you don't select a commission, asset folder creation will fail");
        if(this.props.selectedProjectTemplate===null)
            list.push("You can't create a project without a project template");
        if(this.props.projectFilename===null || this.props.projectFilename==="")
            list.push("You can't create a project without a filename");
        return list;
    }

    render() {
        return(<div>
            <h3>Create new project</h3>
            <p className="information">We will create a new project with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent projectTemplates={this.props.projectTemplates} selectedProjectTemplate={this.props.selectedProjectTemplate}
                              storages={this.props.storages} selectedStorage={this.props.selectedStorage}
                              projectName={this.props.projectName} projectFilename={this.props.projectFilename}
                              selectedWorkingGroupId={this.props.selectedWorkingGroupId}
                              wgList={this.props.wgList}
                              selectedCommissionId={this.props.selectedCommissionId}
                              deletable={this.props.deletable}
                              deep_archive={this.props.deep_archive}
                              sensitive={this.props.sensitive}
            />

            {this.getWarnings().map(warning=><p className="error-text">{warning}</p>)}
            <ErrorViewComponent error={this.state.error}/>
            <LongProcessComponent inProgress={this.state.inProgress} expectedDuration={30} operationName="Project creation"/>
            <span style={{float: "right"}}>
                <button onClick={this.confirmClicked}
                        disabled={this.state.inProgress}
                        style={{color: this.state.inProgress ? "lightgrey" : "black"}}
                >Confirm</button>
            </span>
        </div>)
    }
}

export default ProjectCompletionComponent;
