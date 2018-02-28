import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class TitleEditComponent extends React.Component {
    static propTypes = {
        match: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            loading: false,
            projectName: "",
            error:null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    confirmClicked(){
        const projectId = this.props.match.params.itemid;

        //note - this won't null out an existing vsid
        axios.put("/api/project/" + projectId + "/title", {title: this.state.projectName, vsid: null})
            .then(response=>{
                this.props.history.push("/project")
            })
            .catch(error=>{
                console.error(error);
                this.setState({error: error});
            })
    }

    componentWillMount(){
        const projectId = this.props.match.params.itemid;
        axios.get("/api/project/" + projectId)
            .then(response=>this.setState({projectName: response.data.result.title }))
            .catch(error=>this.setState({error: error}));

        this.setState({projectName: this.props.projectName});
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
                </tbody>
            </table>

            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "left"}}><button onClick={()=>this.props.history.goBack()}>Back</button></span>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default TitleEditComponent;