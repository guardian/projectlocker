import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import MultistepComponentLoadsOnMount from '../common/LoadsOnMount.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class ProjectTypeComponent extends MultistepComponentLoadsOnMount{
    /* CommonMultistepComponent includes an implementation of ComponentDidUpdate which
    updates the parent with our state
     */
    constructor(props){
        super(props);

        this.endpoint = "/api/projecttype";

        this.state = {
            name: null,
            opensWith: null,
            version: null,
            fileExtension: null,
            loading: false,
            error: null
        }
    }

    receivedExistingObject(projectType, cb){
        /* called by the superclass when we get data back for an object */
        console.log("receivedExistingObject");
        this.setState({
            name: projectType.result.name,
            version: projectType.result.targetVersion,
            opensWith: projectType.result.opensWith,
            fileExtension: projectType.result.fileExtension
        }, cb);
    }

    render() {
        if(this.state.error) return <ErrorViewComponent error={this.state.error}/>;

        if(this.state.loading) return <p>Loading...</p>;

        return <div>
            <h3>Project Type</h3>
            <table>
                <tbody>
                <tr>
                    <td>Name of project type</td>
                    <td><input id="project_type_name" className="inputs" value={this.state.name}
                               onChange={event=>this.setState({name: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Opens with which Mac app?</td>
                    <td><input id="opens_with" className="inputs" value={this.state.opensWith}
                               onChange={event=>this.setState({opensWith: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Minimum required software version to open it</td>
                    <td><input id="version" className="inputs" value={this.state.version}
                               onChange={event=>this.setState({version: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>File extension for this file type</td>
                    <td><input id="extension" className="inputs" value={this.state.fileExtension}
                               onChange={event=>this.setState({fileExtension: event.target.value})}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default ProjectTypeComponent;