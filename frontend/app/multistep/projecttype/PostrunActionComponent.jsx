import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import PropTypes from 'prop-types';
import PostrunActionSelector from '../postrun/PostrunActionSelector.jsx';

class PostrunActionComponent extends CommonMultistepComponent {
    static propTypes = {
        actionsList: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        loadErrors: PropTypes.object,
        selectedEntries: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

    }

    render(){
        if(this.props.loadErrors) return <ErrorViewComponent error={this.state.loadErrors}/>;

        return <div>
            <h3>Postrun Actions</h3>
            <p>Which postrun actions should be called when creating this kind of project?</p>
            <PostrunActionSelector actionsList={this.props.actionsList} valueWasSet={this.props.valueWasSet}
                                selectedEntries={this.props.selectedEntries}/>
        </div>
    }
}

export default PostrunActionComponent;