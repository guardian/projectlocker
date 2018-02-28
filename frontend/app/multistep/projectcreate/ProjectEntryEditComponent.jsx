import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import {validateVsid} from "../../validators/VsidValidator.jsx";

class ProjectEntryEditComponent extends React.Component {
    static propTypes = {
        match: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            loading: false,
            projectName: "",
            vsid: "",
            vsidInvalid: false,
            removeVsid: false,
            vsidChanged: false,
            error:null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
        this.vsidUpdated = this.vsidUpdated.bind(this);
    }

    confirmClicked(){
        const projectId = this.props.match.params.itemid;

        let finalPromise =
            axios.put("/api/project/" + projectId + "/title", {title: this.state.projectName, vsid: this.state.vsid})
                .then(response=>{})
                .catch(error=>{
                    console.error(error);
                    this.setState({error: error});
                });

        if(this.state.vsidChanged && (!this.state.vsidInvalid || this.state.removeVsid))
            finalPromise = finalPromise.then(axios.put("/api/project/" + projectId + "/vsid",
                {title: this.state.projectName, vsid: this.state.removeVsid ? null : this.state.vsid})
            .then(response=>{})
            .catch(error=>{
                console.error(error);
                this.setState({error: error});
            }));

       finalPromise.then(()=>this.props.history.push("/project"));
    }

    componentWillMount(){
        const projectId = this.props.match.params.itemid;
        axios.get("/api/project/" + projectId)
            .then(response=>this.setState({projectName: response.data.result.title,vsid: response.data.result.vidispineId },
                ()=>this.vsidUpdated({target:{value:response.data.result.vidispineId }})))
            .catch(error=>this.setState({error: error}));

        this.setState({projectName: this.props.projectName});
    }

    vsidUpdated(event){
        this.setState({vsidChanged: true, vsid: event.target.value, vsidInvalid: (validateVsid(event.target.value)!==null)});
    }

    render(){
        //FIXME: this css class should be renamed
        return(<div className="filter-list-block">
            <h3>Edit project information</h3>
            <p className="information">The only part of the project information that it's possible to edit is the title.</p>
            <p className="information">Press "Confirm" to go ahead, or press Back to cancel.</p>
            <table style={{width: "100%"}}>
                <tbody>
                <tr>
                    <td style={{width: "20%"}}><label htmlFor="project-name">Project name:</label></td>
                    <td><input id="project-name" value={this.state.projectName}
                               onChange={(event)=>this.setState({projectName: event.target.value})}
                               style={{width: "100%" }}/> </td>
                </tr>
                <tr>
                    <td style={{width: "20%"}}><label htmlFor="vsid-input">Vidispine ID: </label></td>
                    <td><input id="vsid" value={this.state.vsid}
                               onChange={this.vsidUpdated}
                               disabled={this.state.removeVsid}
                               style={{width: "95%"}}/>
                            {this.state.vsidInvalid ?
                                <i className="fa fa-exclamation" style={{color: "red", marginLeft: "0.5em"}}/> :
                                <i className="fa fa-check" style={{color: "green", marginLeft: "0.5em"}}/>}
                    </td>
                </tr>
                <tr>
                    <td><label htmlFor="vsid-remove">Remove vidispine ID:</label></td>
                    <td><input type="checkbox" id="remove-vsid" value={this.removeVsid}
                               onChange={(event)=>this.setState({removeVsid: event.target.value})}/>
                    </td>
                </tr>
                </tbody>
            </table>

            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "left"}}><button onClick={()=>this.props.history.goBack()}>Back</button></span>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectEntryEditComponent;